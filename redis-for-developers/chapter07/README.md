### CHAPTER 7 레디스 데이터 백업 방법

레디스는 인메모리라서 **재시작하면 데이터가 사라진다.** 이를 막는 영속화 수단이 **RDB(스냅샷)** 와 **AOF(쓰기 로그)** 다. 2장에서 설정값만 훑었다면, 7장은 **동작 원리**를 판다. 특히 *복구 시 어느 쪽이 우선인가*, *SAVE와 BGSAVE의 차이*, *AOF Rewrite가 버전별로 어떻게 달라졌는가*, *AOF가 OS를 거쳐 디스크에 닿는 경로*가 핵심이다.

> 이 장의 일부 동작(복구 우선순위, Multi Part AOF 파일 구조)은 **로컬 Redis 7.2.6에서 직접 실행해 확인**했다. 확인한 내용은 `✅ 검증` 으로 표시.

#### 목차

- [1부. RDB vs AOF 개요와 장단점](#1부-rdb-vs-aof-개요와-장단점)
- [2부. RDB — SAVE vs BGSAVE](#2부-rdb--save-vs-bgsave)
- [3부. AOF — 동작과 Rewrite](#3부-aof--동작과-rewrite)
- [4부. AOF Rewrite의 버전별 재구성 방식](#4부-aof-rewrite의-버전별-재구성-방식)
- [5부. AOF 쓰기와 운영체제 — write / fsync 흐름](#5부-aof-쓰기와-운영체제--write--fsync-흐름)
- [6부. 복구 시 우선순위 — AOF가 우선인 이유](#6부-복구-시-우선순위--aof가-우선인-이유)
- [7부. 운영 권장 구성과 체크리스트](#7부-운영-권장-구성과-체크리스트)

---

## 1부. RDB vs AOF 개요와 장단점

| | RDB (Redis Database) | AOF (Append Only File) |
|---|---|---|
| **저장 방식** | 특정 시점의 **전체 데이터 스냅샷**(바이너리) | **쓰기 명령을 순서대로 로그**로 추가 |
| **기록 시점** | 주기적(`save` 조건) / 수동(`BGSAVE`) | 명령 실행마다 버퍼에 추가 → `appendfsync` 정책에 따라 디스크 반영 |
| **파일 모양** | 압축된 바이너리 (`dump.rdb`) | 명령 텍스트(RESP) 로그 (7.0+ 는 디렉터리 + 여러 파일) |
| **데이터 유실 범위** | 마지막 스냅샷 이후 전부 (분 단위) | `everysec` 기준 **최대 약 1초** (`always`면 거의 0) |
| **파일 크기** | **작음** (압축, 최종 상태만) | **큼** (이력이 쌓임 → Rewrite로 압축) |
| **복구(로딩) 속도** | **빠름** (바이너리를 그대로 적재) | 느림 (명령 재실행; RDB preamble이면 앞부분은 빠름) |
| **쓰기 성능 영향** | 평시 거의 없음 (fork 순간만) | 지속적인 디스크 쓰기 + fsync 부담 |
| **백업/이관** | 파일 하나 복사하면 끝, 버전 간 이관 용이 | 상대적으로 번거로움 |
| **가독성/수동 복구** | 불가 | 텍스트라 **잘못된 명령(FLUSHALL 등) 제거 후 복구** 가능 |
| **손상 대응** | 단일 파일, 손상 시 복구 어려움 | 끝부분 손상은 `redis-check-aof --fix` 로 복구 가능 |

### 장단점 요약

**RDB**
- ✅ 파일 작고 로딩 빠름, 백업·DR(원격 복사)에 적합, 평시 성능 영향 작음
- ❌ **스냅샷 사이 데이터 유실**, 데이터가 크면 fork 지연 + COW 메모리 증가

**AOF**
- ✅ **유실 최소화**, 사람이 읽을 수 있는 로그, 실수 명령 제거 후 복구 가능
- ❌ 파일 비대, 복구 느림, 디스크 쓰기 부담, Rewrite 필요

> 둘은 **대체재가 아니라 보완재**. 권장은 **둘 다 켜기**(7부).

---

## 2부. RDB — SAVE vs BGSAVE

### 두 명령 비교

| | `SAVE` | `BGSAVE` |
|---|---|---|
| **실행 주체** | **메인 스레드가 직접** 저장 | `fork()`로 **자식 프로세스**가 저장 |
| **블로킹** | **저장 끝날 때까지 모든 명령 차단** | 부모는 계속 명령 처리 (fork 순간만 잠깐 멈춤) |
| **데이터 크기 영향** | 클수록 수 초~수십 초 서비스 중단 | 부모는 영향 적음 (단, fork·COW 비용) |
| **운영 사용** | **금지에 가까움** (종료 직전·오프라인 작업 정도) | 표준 |
| **중복 실행** | — | 이미 진행 중이면 에러/예약 |

```
SAVE      : [메인 스레드] ───── 스냅샷 쓰는 동안 전부 정지 ─────▶

BGSAVE    : [부모]  ──fork──▶ 계속 명령 처리 ────────────────────▶
                      │
            [자식]    └──▶ 스냅샷 기록 → 임시파일 → rename(dump.rdb) → 종료
```

### BGSAVE 내부 동작

1. 부모가 **`fork()`** → 자식은 fork 시점의 **메모리 스냅샷**을 가짐
2. 자식이 **임시 파일**(`temp-<pid>.rdb`)에 기록
3. 완료되면 `dump.rdb`로 **원자적 `rename`** (기록 도중 크래시해도 기존 파일은 안전)
4. 자식 종료, 부모는 `rdb_last_save_time` 갱신

### Copy-on-Write (COW)와 메모리

- fork 직후 부모·자식은 **같은 물리 페이지를 공유**
- 부모가 쓰기를 하면 **해당 페이지만 복사**(COW) → 쓰기가 많을수록 추가 메모리 증가
- **최악의 경우 메모리 사용량이 2배**까지 → `maxmemory`를 물리 메모리의 100%로 잡으면 안 되는 이유
- **fork 자체의 지연**: 페이지 테이블 복사 비용이 **메모리 크기에 비례** (수십 GB면 수백 ms~초 단위 정지 가능)
- **THP(Transparent Huge Pages)** 가 켜져 있으면 COW 단위가 4KB → **2MB**로 커져 메모리·지연이 급증 → **비활성화 권장**
- `vm.overcommit_memory = 1` 이 아니면 fork가 메모리 부족으로 실패할 수 있음

### RDB가 트리거되는 경우

| 트리거 | 방식 |
|---|---|
| `save <초> <변경수>` 조건 충족 | 자동 `BGSAVE` |
| `BGSAVE` 수동 실행 | 자식 프로세스 |
| **정상 종료**(`SHUTDOWN`, SIGTERM) | save 설정이 있으면 **동기 SAVE** 후 종료 (`SHUTDOWN NOSAVE`로 생략) |
| **레플리카 최초/전체 동기화** | 마스터가 RDB 생성 (디스크 방식 BGSAVE 또는 diskless 전송) |

### 확인 명령

```
LASTSAVE                      # 마지막 성공 저장 시각 (unix time)
INFO persistence
# rdb_bgsave_in_progress:0
# rdb_last_bgsave_status:ok
# rdb_last_save_time:...
# latest_fork_usec:...        ← 최근 fork 소요 시간(μs). 모니터링 핵심
```

---

## 3부. AOF — 동작과 Rewrite

### 기록 흐름

```
클라이언트 쓰기 명령
   ↓ 1. 메모리 데이터 갱신 (명령 실행)
   ↓ 2. AOF 버퍼(aof_buf)에 명령 추가
   ↓ 3. 이벤트 루프 종료 직전(beforeSleep)에 write() 로 파일에 기록
   ↓ 4. appendfsync 정책에 따라 fsync()로 디스크 반영   ← 5부에서 상세
```

> 레디스는 **실행한 뒤에 로그를 쓴다**(Write-Ahead가 아님). 그래서 명령 문법 검증 비용이 없고, 잘못된 명령은 로그에 남지 않는다. 반대로 **실행 직후 ~ 기록 전에 죽으면 해당 명령은 유실**.

### 왜 Rewrite가 필요한가

같은 키를 수천 번 갱신하면 로그에는 수천 줄이 쌓이지만, **최종 상태를 만드는 명령은 1개**면 충분하다.

```
(Rewrite 전)                          (Rewrite 후)
SET counter 1                         SET counter 1000
INCR counter  × 999회        ──▶      (최종 상태를 만드는 최소 명령)
RPUSH list a / RPUSH list b / ...     RPUSH list a b c ...
```

- **핵심: Rewrite는 기존 AOF 파일을 읽어서 줄이는 게 아니라, 현재 메모리 데이터를 기준으로 새로 쓴다.**
- 그래서 시간이 지나 파일이 비대해져도 **메모리 크기에 수렴**하는 새 파일이 만들어진다.

### Rewrite 트리거

| 방식 | 설명 |
|---|---|
| **수동** | `BGREWRITEAOF` (fork 기반 백그라운드) |
| **자동** | 현재 AOF 크기가 **마지막 Rewrite 후 크기 대비 `auto-aof-rewrite-percentage`(기본 100%)** 이상 증가 **AND** `auto-aof-rewrite-min-size`(기본 64mb) 이상 |
| 부가 | AOF를 **처음 켤 때**(`CONFIG SET appendonly yes`) 초기 데이터셋으로 한 번 생성 |

---

## 4부. AOF Rewrite의 버전별 재구성 방식

Rewrite 중에도 **클라이언트의 쓰기는 계속 들어온다.** 이 "Rewrite 도중 들어온 쓰기"를 어떻게 처리하느냐가 버전별 핵심 차이다.

### 4.1 Redis 7.0 이전 — 단일 파일 + Rewrite 버퍼

파일: `appendonly.aof` **하나**.

```
1. 부모 fork → 자식이 메모리 스냅샷으로 새 파일(temp-rewriteaof-bg-<pid>.aof) 작성
2. 그동안 부모는
   (a) 기존 appendonly.aof 에 계속 기록 (이중 기록; 자식이 실패해도 안전하게)
   (b) 새 쓰기를 "AOF Rewrite 버퍼"(메모리)에도 쌓음
3. 자식 완료 → 부모가 Rewrite 버퍼 내용을 새 파일 끝에 append (메인 스레드에서 수행)
4. 새 파일을 appendonly.aof 로 rename (원자적 교체)
```

**문제점**
- **쓰기 이중화**: Rewrite 동안 같은 쓰기를 디스크(기존 AOF)와 메모리 버퍼(Rewrite 버퍼)에 중복 처리 → 메모리·I/O 낭비
- **마지막 append가 메인 스레드에서 수행**되어, 쓰기가 많았다면 **지연 스파이크**
- Rewrite 버퍼가 **메모리를 추가 소비**
- 임시 파일 · rename · 기존 파일 정리 과정이 하나의 파일에 의존

> 4.0~6.2에서는 부모가 자식에게 **pipe로 증분 쓰기를 전달**해 자식이 직접 반영하게 하여 마지막 append 양을 줄이는 개선이 있었으나, 기본 구조(단일 파일 + 부모 측 Rewrite 버퍼)는 동일.

### 4.2 Redis 4.0+ — RDB Preamble (`aof-use-rdb-preamble`)

단일 파일 AOF의 **앞부분을 RDB 포맷**으로 쓰는 하이브리드.

```
[ RDB 바이너리: Rewrite 시점의 스냅샷 ][ AOF 명령 로그: 그 이후 쓰기 ]
```

- 복구 시 앞부분은 **RDB 속도**로 적재, 뒷부분만 명령 재실행 → **로딩 시간 대폭 단축**
- 5.0부터 기본값 `yes`

### 4.3 Redis 7.0+ — Multi Part AOF (MP-AOF)

단일 파일을 **디렉터리 + 여러 파일 + manifest**로 재설계. Rewrite 버퍼 자체를 없앴다.

| 파일 | 역할 |
|---|---|
| **base** (`appendonly.aof.N.base.rdb` / `.base.aof`) | Rewrite가 만든 **기준 스냅샷** (preamble `yes`면 `.rdb`, `no`면 `.aof`) |
| **incr** (`appendonly.aof.N.incr.aof`) | base 이후의 **증분 명령 로그** (여러 개 가능) |
| **manifest** (`appendonly.aof.manifest`) | 어떤 파일들이 유효한 AOF인지 목록·순서 (원자적으로 갱신) |

```
appendonlydir/
├── appendonly.aof.1.base.rdb      ← 기준 스냅샷
├── appendonly.aof.1.incr.aof      ← 이후 증분 로그
└── appendonly.aof.manifest        ← 파일 목록
```

✅ **검증 (Redis 7.2.6, `appendonly yes` 기동 직후)**

```
$ cat appendonlydir/appendonly.aof.manifest
file appendonly.aof.1.base.rdb seq 1 type b
file appendonly.aof.1.incr.aof seq 1 type i
```

**7.0+ Rewrite 동작**

```
1. BGREWRITEAOF 시작 → 부모는 즉시 "새 incr 파일"을 열고 이후 쓰기를 거기에 기록
2. 자식이 메모리 스냅샷으로 "새 base 파일" 작성 (부모와 독립)
3. 자식 완료 → 부모가 manifest를 원자적으로 갱신 (새 base + 새 incr 가리킴)
4. 이전 base/incr 파일은 백그라운드에서 삭제
```

✅ **검증 (`BGREWRITEAOF` 실행 후)**

```
$ cat appendonlydir/appendonly.aof.manifest
file appendonly.aof.2.base.rdb seq 2 type b      ← seq 1 → 2 로 교체
file appendonly.aof.2.incr.aof seq 2 type i
```

`aof-use-rdb-preamble no` 로 바꾸면 base가 `appendonly.aof.2.base.aof` (명령 텍스트) 로 생성됨도 확인.

**개선점**
- **Rewrite 버퍼 제거** → 메모리 절약, 마지막 append로 인한 **지연 스파이크 제거**
- 쓰기 이중화 제거 (부모는 새 incr에만 기록)
- 파일 교체가 **manifest 갱신 한 번**으로 원자적
- 기존 파일 삭제를 백그라운드로 분리

### 버전별 비교

| 축 | ~6.2 | 7.0+ |
|---|---|---|
| **파일 구조** | `appendonly.aof` 1개 | `appendonlydir/` + base + incr + manifest |
| **Rewrite 중 쓰기** | 기존 AOF + **Rewrite 버퍼** 이중 | **새 incr 파일**에만 |
| **마무리 방식** | 버퍼 append + rename | **manifest 원자 갱신** |
| **메모리 오버헤드** | Rewrite 버퍼만큼 증가 | 없음 |
| **지연 스파이크** | 마무리 append 시 가능 | 크게 감소 |
| **RDB preamble** | 4.0+ (5.0 기본 on) | base 파일 형식으로 유지 (`.rdb`/`.aof`) |
| **업그레이드** | — | 기존 단일 `appendonly.aof` 를 7.0이 **자동으로 새 구조로 인식·변환** |

---

## 5부. AOF 쓰기와 운영체제 — write / fsync 흐름

AOF의 내구성은 **"레디스가 쓴 것"과 "디스크에 실제로 닿은 것"이 다르다**는 점을 이해해야 정확히 알 수 있다.

### 데이터가 거치는 계층

```
┌────────────────────────────────────────────────────────┐
│ ① Redis 프로세스 메모리                                    │
│    명령 실행 → aof_buf (유저 공간 버퍼)                    │
└───────────────┬────────────────────────────────────────┘
                │ write(fd, buf)   ← 시스템 콜
                ▼
┌────────────────────────────────────────────────────────┐
│ ② OS 커널 페이지 캐시 (Page Cache)                         │
│    "쓴 것처럼" 보이지만 아직 디스크 아님 (dirty page)       │
└───────────────┬────────────────────────────────────────┘
                │ fsync(fd)  ← 명시적 flush, 또는 OS가 주기적으로 writeback
                ▼
┌────────────────────────────────────────────────────────┐
│ ③ 디스크 컨트롤러 캐시 → ④ 실제 저장 매체                   │
└────────────────────────────────────────────────────────┘
```

- `write()` 가 성공해도 데이터는 **커널 페이지 캐시**에 있을 뿐, 전원이 나가면 사라질 수 있음
- `fsync()` 가 **디스크까지 내리도록 강제**하는 유일한 수단 (느림: ms 단위)

### appendfsync 정책별 동작

| 정책 | write() | fsync() | 유실 가능 범위 | 성능 |
|---|---|---|---|---|
| **always** | 명령마다 | **명령마다(메인 스레드, 응답 전)** | 거의 0 | 가장 느림 |
| **everysec** (기본) | 이벤트 루프마다 | **1초마다, 백그라운드 스레드(BIO)** | 최대 ~1초 (최악 ~2초) | 좋음 |
| **no** | 이벤트 루프마다 | **호출 안 함 (OS에 맡김)** | OS writeback 주기(리눅스 보통 수십 초) | 가장 빠름 |

### 장애 유형별 유실 범위 (중요)

| 장애 | 페이지 캐시 상태 | everysec / no 일 때 |
|---|---|---|
| **Redis 프로세스만 죽음** (kill -9, OOM kill) | **커널에 이미 있음 → 살아남음** | `write()` 까지 끝난 데이터는 **유실 없음** (aof_buf에만 있던 극소량만 유실) |
| **OS 크래시 / 전원 장애** | 사라짐 | fsync 안 된 구간 유실 (everysec ≈ 1초, no ≈ 수십 초) |

> 즉 **프로세스 장애만 놓고 보면 `everysec`도 사실상 유실이 거의 없다.** `always`가 의미 있는 건 **OS/전원 장애**까지 막고 싶을 때.

### everysec의 숨은 지연 — fsync가 느릴 때

- fsync는 **백그라운드 스레드**가 수행하므로 보통 메인 스레드를 막지 않음
- 하지만 디스크가 느려 **직전 fsync가 아직 안 끝났는데** 새 `write()`를 하면 같은 파일에 대해 블록될 수 있어, 레디스는 **최대 2초까지 write를 미루는** 동작을 함 → 유실 가능 범위가 최악 **~2초**
- `INFO persistence` 의 `aof_delayed_fsync` 카운터가 증가하면 **디스크 I/O 병목 신호**

### Rewrite/BGSAVE와 디스크 경쟁

- Rewrite·BGSAVE의 대량 쓰기가 **같은 디스크에서 AOF fsync와 경쟁** → fsync 지연 → 서비스 지연
- `no-appendfsync-on-rewrite yes`: Rewrite 중에는 **AOF fsync를 일시 생략**해 경쟁을 줄임 (대신 그 구간 유실 범위가 커짐 — 리눅스 기본 설정 기준 최대 수십 초)
- `aof-rewrite-incremental-fsync yes`(기본): Rewrite 파일을 **32MB 단위로 점진 fsync** → 한꺼번에 flush 되며 생기는 지연 방지
- 가능하면 **AOF 전용 디스크/볼륨** 분리, 고성능 SSD 사용

### 운영체제와의 연관관계 정리

| OS 요소 | 레디스와의 관계 |
|---|---|
| **페이지 캐시** | `write()`가 도달하는 1차 목적지. 프로세스 장애는 방어, OS 장애는 못 막음 |
| **`fsync`** | 내구성의 최종 보증. `appendfsync` 가 호출 빈도를 결정 |
| **`vm.dirty_*` (writeback)** | `appendfsync no` 일 때 OS가 언제 디스크에 쓰는지 결정 |
| **`fork()` + COW** | BGSAVE/Rewrite의 기반. 메모리 크기에 따라 fork 지연·메모리 2배 위험 |
| **`vm.overcommit_memory=1`** | fork 실패 방지 (권장) |
| **THP** | COW 단위를 2MB로 키워 지연/메모리 악화 → 끄기 |
| **파일시스템 `rename`** | RDB/AOF 파일 **원자적 교체**를 가능하게 하는 전제 |
| **디스크 I/O 스케줄링** | Rewrite/BGSAVE와 fsync가 경쟁 → 지연의 원인 |

---

## 6부. 복구 시 우선순위 — AOF가 우선인 이유

### 규칙

> **`appendonly yes` 이면 AOF로 복구하고, RDB 파일(`dump.rdb`)은 무시한다.** `appendonly no` 이면 RDB로 복구한다.

✅ **검증 (Redis 7.2.6)** — `dump.rdb`에는 `k1`만, AOF에는 `k1, k2, k3`가 있는 상태에서 재시작:

```
appendonly yes  → keys: k1 k2 k3     (AOF 사용)
appendonly no   → keys: k1           (RDB 사용, 더 오래된 스냅샷)
```

로그: `Reading RDB base file on AOF loading... / DB loaded from append only file`

### 왜 AOF가 우선인가

1. **더 최신·더 완전하다**: RDB는 마지막 스냅샷 시점까지만, AOF는 `everysec` 기준 **직전 1초까지** 포함
2. **AOF가 켜져 있다는 것 = 운영자가 유실 최소화를 원한다는 의도**
3. 둘의 최신성을 **런타임에 비교·병합하는 건 불가능/위험** → "켜진 쪽을 신뢰"하는 단순 규칙이 안전
4. 일반적으로 RDB의 내용은 AOF에 **이미 포함**되어 있음 (7.0+ AOF의 base 자체가 RDB 스냅샷)

### ⚠️ 함정 — 기존 RDB가 있는 인스턴스에 AOF를 "재시작으로" 켜면 데이터가 사라진다

✅ **검증 (Redis 7.2.6)**: `dump.rdb`(k1,k2)가 있고 `appendonlydir`는 없는 상태에서 `--appendonly yes` 로 **재시작**

```
$ redis-cli dbsize
0                                   ← dump.rdb 의 데이터가 로드되지 않음!
```

- AOF가 켜져 있으면 **AOF만 보고**, AOF 파일이 없으면 **빈 상태로 시작** → 그 상태로 새 AOF를 만들어버림
- 이후 빈 AOF가 기록되며 사실상 **RDB 데이터 접근 불가**(파일은 남아있지만 로드 안 됨)

**안전한 AOF 활성화 절차**
```
# ✅ 런타임에 켜기 — 현재 메모리 데이터로 AOF를 즉시 생성
CONFIG SET appendonly yes
# (Rewrite 완료 확인 후) redis.conf 에도 appendonly yes 반영 → CONFIG REWRITE
```
- 재시작으로 켜야 한다면: 먼저 **AOF 파일을 만든 뒤**(런타임 활성화) 재시작

### 복구 순서 요약

```
서버 시작
  ├─ appendonly yes ─▶ AOF(base + incr) 로드 ─▶ 완료
  │                    (파일 없으면 빈 상태로 시작 ⚠️)
  └─ appendonly no  ─▶ dump.rdb 로드 ─▶ 완료
```

### AOF 손상 시

- 끝부분이 잘린 경우: `aof-load-truncated yes`(기본)면 **경고 후 가능한 부분까지 로드**
- 중간 손상: `redis-check-aof --fix <file>` 로 손상 지점 이후 제거 후 복구
- 7.0+ 는 `redis-check-aof` 를 **manifest 또는 개별 파일**에 대해 실행

---

## 7부. 운영 권장 구성과 체크리스트

### 요구사항별 선택

| 요구사항 | 구성 |
|---|---|
| 순수 캐시, 유실 무관 | 영속화 끔 (또는 RDB만) |
| 재시작 시 웜 상태 유지 정도 | RDB만 (`save` 주기 조정) |
| 유실 ~1초 허용, 일반 서비스 | **AOF `everysec` + RDB 병행** (권장) |
| 유실 최소화 (OS/전원 장애 포함) | AOF `always` (성능 희생) |
| 백업/DR | **RDB를 주기적으로 원격·오브젝트 스토리지로 복사** |

### 권장 설정 예

```
appendonly yes
appendfsync everysec
aof-use-rdb-preamble yes
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
no-appendfsync-on-rewrite no          # 디스크 경합 심하면 yes 검토
save 3600 1 300 100 60 10000          # RDB도 병행 (백업·복제용)
```

### 체크리스트

- [ ] **`latest_fork_usec`** 모니터링 (fork 지연), 인스턴스 메모리가 클수록 위험
- [ ] **THP 비활성화**, `vm.overcommit_memory=1`
- [ ] 메모리 여유: **fork + COW 대비 `maxmemory`를 물리 메모리의 60~75%** 로
- [ ] **`aof_delayed_fsync`** 증가 시 디스크 병목 의심
- [ ] AOF 디스크 **여유 공간** 확인 (Rewrite 시 base 신규 생성 → 일시적으로 파일 2벌)
- [ ] **백업은 RDB 복사** — `BGSAVE` 후 `dump.rdb` 를 원격에 보관, **복구 리허설** 주기적 수행
- [ ] 기존 RDB 인스턴스에 AOF 켤 때는 **`CONFIG SET appendonly yes`** (재시작으로 켜지 않기)
- [ ] 레플리카에서 영속화를 담당시켜 **마스터의 fork/디스크 부담 분리** (단, 마스터 재시작 시 빈 데이터로 복제되어 레플리카가 비워지는 사고 주의 → 마스터도 영속화 유지)

---

### 요약

- **RDB**: 시점 스냅샷, 작고 빠른 복구·백업에 강하지만 **스냅샷 간 유실**. **AOF**: 쓰기 로그, 유실 최소(≈1초)지만 **파일 크고 복구 느림**. 보완재로 **둘 다 사용** 권장
- **SAVE**는 메인 스레드가 직접 저장해 **전체 차단**, **BGSAVE**는 `fork()` 자식이 저장(**COW**). fork 지연·메모리 2배·THP 주의
- **AOF Rewrite**는 기존 로그를 줄이는 게 아니라 **현재 메모리 기준으로 새로 작성**
- **버전별 Rewrite**: ~6.2는 단일 파일 + **Rewrite 버퍼**(이중 기록, 마무리 append 지연) → 4.0+ **RDB preamble**로 로딩 가속 → **7.0 Multi Part AOF**(base + incr + manifest)로 **버퍼 제거·원자적 교체**
- **OS 흐름**: `aof_buf → write() → 페이지 캐시 → fsync() → 디스크`. **프로세스 장애는 페이지 캐시가 방어**, OS/전원 장애는 fsync 여부가 결정. `everysec`은 백그라운드 fsync, 느리면 최악 ~2초
- **복구 우선순위**: `appendonly yes` → **AOF 우선**(더 최신·완전, 켠 의도 존중), `no` → RDB. ✅ 7.2.6 검증
- **함정**: RDB만 있는 인스턴스를 `appendonly yes`로 **재시작하면 빈 상태로 시작**(✅ 검증) → 반드시 **`CONFIG SET appendonly yes`** 로 활성화
