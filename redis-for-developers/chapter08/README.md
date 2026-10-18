### CHAPTER 8 복제

마스터의 데이터를 **레플리카에 복사해 두는** 기능. 읽기 분산과 **고가용성(센티널/클러스터 failover의 전제)**, 그리고 마스터 장애 시 데이터 보호의 기반이다. 핵심은 **세 단계**(초기 전체 동기화 → 지속적 명령 전파 → 끊겼다 복구 시 부분 재동기화)와, 초기 전체 동기화를 **디스크를 거치느냐 / 거치지 않느냐**로 나눠 이해하는 것.

> 동작은 **로컬 Redis 7.2.6에서 마스터·레플리카를 직접 띄워 로그로 확인**했다. 확인한 부분은 `✅ 검증` 표시.
>
> "비동기로 할 때"는 두 가지 층위로 나눠 정리했다. ① **복제 자체가 비동기**(명령 전파) ② 디스크를 거치지 않는 **Diskless**(스트리밍 전송). 4~5부 참고.

#### 목차

- [1부. 복제 개요와 용어](#1부-복제-개요와-용어)
- [2부. 전체 동기화 (Full Resync) — 디스크 사용 vs 미사용](#2부-전체-동기화-full-resync--디스크-사용-vs-미사용)
- [3부. 케이스별 비교](#3부-케이스별-비교)
- [4부. 지속적 복제 — 비동기 명령 전파](#4부-지속적-복제--비동기-명령-전파)
- [5부. 비동기의 한계와 보완 (WAIT, min-replicas)](#5부-비동기의-한계와-보완-wait-min-replicas)
- [6부. 부분 재동기화 (Partial Resync)](#6부-부분-재동기화-partial-resync)
- [7부. 설정 정리](#7부-설정-정리)
- [8부. 운영 이슈와 체크리스트](#8부-운영-이슈와-체크리스트)

---

## 1부. 복제 개요와 용어

### 구조

```
        ┌──────────┐   쓰기 / 읽기
 Client ▶│  Master  │
        └────┬─────┘
             │ 복제 스트림 (단방향)
      ┌──────┴───────┐
      ▼              ▼
 ┌─────────┐    ┌─────────┐
 │Replica 1│    │Replica 2│   ← 기본 읽기 전용 (replica-read-only yes)
 └─────────┘    └─────────┘
```

- **단방향**: 마스터 → 레플리카. 레플리카는 마스터를 **클라이언트처럼** 바라보며 명령을 받아 실행
- **마스터 1 : 레플리카 N**, 레플리카의 레플리카(**체인 복제**)도 가능
- 레플리카도 **복제 ID + 오프셋**을 갖고 있어 **언제든 마스터가 될 수 있는** 준비 상태
- 시작: `REPLICAOF <host> <port>` (또는 설정 `replicaof`), 해제: `REPLICAOF NO ONE` (마스터로 승격)

### 핵심 용어

| 용어 | 설명 |
|---|---|
| **Replication ID (replid)** | 데이터셋의 "이력"을 식별하는 40자리 ID. failover로 새 마스터가 되면 새로 발급 |
| **Replication Offset** | 마스터가 복제 스트림에 쓴 **누적 바이트 수**. 레플리카가 어디까지 받았는지의 위치 |
| **Replication Backlog** | 마스터가 최근 복제 스트림을 보관하는 **링 버퍼**. 부분 재동기화에 사용 |
| **PSYNC** | `PSYNC <replid> <offset>` — 레플리카가 "이 지점부터 이어서 달라"고 요청하는 명령 |

### 동기화 3단계

```
① 연결/핸드셰이크  PING → AUTH → REPLCONF → PSYNC
② 초기 동기화     Full Resync(RDB 전송)  또는  Partial Resync(backlog 이어받기)
③ 지속 복제       마스터의 쓰기 명령을 레플리카에 계속 전파 (비동기)
```

---

## 2부. 전체 동기화 (Full Resync) — 디스크 사용 vs 미사용

레플리카가 **처음 붙거나**, 부분 재동기화가 불가능할 때 수행. 마스터의 **전체 데이터셋(RDB)** 을 레플리카에 보내는 과정. RDB를 어떻게 전달하느냐에 따라 두 방식으로 나뉜다.

### 공통 흐름

```
1. 레플리카: PSYNC ? -1  (처음이라 replid/offset 없음)
2. 마스터:  +FULLRESYNC <replid> <offset>  응답
3. 마스터:  fork() → 자식이 현재 메모리를 RDB로 직렬화  ← 이 시점의 스냅샷
4. 마스터:  RDB 생성 중 들어오는 쓰기 명령을 레플리카별 버퍼에 쌓음
5. RDB 전송 완료 → 레플리카가 기존 데이터 삭제 후 RDB 로드
6. 마스터:  쌓아둔 버퍼의 명령을 전송 → 이후 실시간 전파로 전환
```

### Case A. 디스크 사용 (Disk-backed) — `repl-diskless-sync no`

```
Master                                        Replica
  │ 1. fork → 자식이 RDB를 디스크(dump.rdb)에 저장
  │ 2. 저장 완료
  │ 3. 부모가 dump.rdb 파일을 소켓으로 전송 ─────────▶ 4. 디스크에 임시 저장
  │ (그 사이 쓰기는 버퍼에 누적)                       5. 기존 데이터 flush → RDB 로드
  │ 6. 누적 버퍼 전송 ──────────────────────────────▶ 7. 반영 → 실시간 복제로 전환
```

✅ **검증 (Redis 7.2.6, 마스터 `save ""` + `appendonly no`)**

```
[master] Starting BGSAVE for SYNC with target: disk
[master] Background saving terminated with success
[replica] MASTER <-> REPLICA sync: receiving 7461 bytes from master to disk
[replica] MASTER <-> REPLICA sync: Flushing old data
[replica] MASTER <-> REPLICA sync: Loading DB in memory
```

- ✅ **영속화를 꺼둔 마스터에도 `dump.rdb`가 생성됨** (디렉터리에서 확인). 동기화용 RDB가 디스크에 남는 것
- 이 파일을 지우려면 `rdb-del-sync-files yes` (영속화가 꺼진 인스턴스에서만 동작)
- **파일 재사용**: 이미 진행 중이거나 막 끝난 BGSAVE의 RDB를 **동시에 붙은 다른 레플리카가 같이 사용**할 수 있음 (같은 시점이면 버퍼도 복사해서 공유)

**장점**
- 마스터에 RDB 파일이 남아 **여러 레플리카가 순차적으로 붙어도 재사용** 가능
- RDB 파일이 있어 **디버깅/수동 복사** 용이
- 구조가 단순하고 오래 검증됨

**단점**
- **마스터 디스크 쓰기 + 읽기** 발생 → 디스크가 느리면 동기화 전체가 느림
- 디스크 I/O가 **AOF fsync와 경쟁** → 서비스 지연 가능
- 저장 → 전송이 **순차**라 전체 시간이 길어짐

### Case B. 디스크 미사용 (Diskless) — `repl-diskless-sync yes` (7.0+ 기본값)

```
Master                                         Replica
  │ 1. (repl-diskless-sync-delay 대기: 다른 레플리카 합류 기다림)
  │ 2. fork → 자식이 RDB를 "소켓으로 직접" 스트리밍 ──▶ 3. 수신 (EOF 마커 방식)
  │    (디스크 쓰기 없음)                              4. 기존 데이터 flush → 로드
  │ 5. 누적 버퍼 전송 ───────────────────────────────▶ 6. 실시간 복제로 전환
```

✅ **검증 (Redis 7.2.6 기본 설정으로 기동)**

```
[master] Delay next BGSAVE for diskless SYNC                  ← 5초 대기 (delay)
[master] Starting BGSAVE for SYNC with target: replicas sockets
[master] Streamed RDB transfer with replica ... succeeded (socket)
[replica] receiving streamed RDB from master with EOF to disk
```

- ✅ 기본값 확인: `repl-diskless-sync yes`, `repl-diskless-sync-delay 5`, `repl-diskless-sync-max-replicas 0`
- **마스터 디스크를 쓰지 않음** (마스터 디렉터리에 `dump.rdb` 없음 확인)
- **`repl-diskless-sync-delay`(기본 5초)**: 첫 레플리카가 붙어도 잠시 기다렸다가, **그 사이 붙은 레플리카들에게 한 번의 전송으로 동시 전달**
  - 전송이 **시작된 뒤** 붙은 레플리카는 **다음 전송 차례까지 대기** (소켓 스트림은 재사용 불가)
  - 0으로 두면 즉시 시작하지만 레플리카가 여러 대일 때 **전송을 여러 번 반복**
- `repl-diskless-sync-max-replicas` (7.0+): 지정한 수만큼 모이면 delay 전에 즉시 시작

**장점**
- 마스터 **디스크 I/O 없음** → 느린 디스크/AOF와의 경합 없음, **빠른 네트워크 + 느린 디스크** 환경에 유리
- 생성과 전송이 **파이프라이닝** (동시에 진행) → 전체 시간 단축 가능

**단점**
- **시작 지연**(delay) 발생
- 레플리카가 여러 대 **시차를 두고** 붙으면 **전송을 반복** (파일 재사용 불가)
- 전송 중 **연결이 끊기면 처음부터** 다시 (파일이 없어 이어받기 불가)

### Case B-2. 레플리카 측 디스크 사용 여부 — `repl-diskless-load`

마스터의 방식과 **별개로**, 레플리카가 받은 RDB를 **디스크를 거쳐 로드하느냐**도 선택할 수 있다.

| `repl-diskless-load` | 동작 | 특징 |
|---|---|---|
| **disabled** (기본) | 받은 RDB를 **임시 파일로 저장 → 로드** | 안전. ✅ 위 로그의 `EOF to disk` |
| **on-empty-db** | 레플리카가 **비어 있을 때만** 소켓에서 바로 메모리로 파싱 | 비어있지 않으면 disabled처럼 동작 |
| **swapdb** | **기존 데이터를 백업해 둔 채** 소켓 → 메모리로 바로 로드, 성공 시 교체 | 로드 중에도 **구 데이터로 서비스 가능**. **메모리 약 2배 필요** |

✅ **검증 (`--repl-diskless-load swapdb`)**

```
[replica] receiving streamed RDB from master with EOF to parser    ← disk 아님
[replica] Done loading RDB, keys loaded: 500
[replica] Discarding old DB in background                          ← 구 DB 교체
→ 마스터/레플리카 양쪽 디렉터리에 RDB 파일 없음 (완전 diskless)
```

- **마스터 diskless + 레플리카 swapdb** = 양쪽 모두 디스크를 쓰지 않는 **완전 무디스크 복제**
- 주의: 로딩 중 연결이 끊기면 **롤백** 후 구 데이터 유지 (swapdb) — 안전하지만 메모리 여유 필수

### 레플리카가 로딩하는 동안

- 기존 데이터를 **flush 후 로드**하는 동안(기본 방식) 레플리카는 **서비스 불가 상태** (`replica-serve-stale-data yes` 여도 로딩 중에는 `-LOADING` 에러)
- 큰 데이터셋은 **로드에 수 분** 걸릴 수 있음 → 레플리카 읽기 분산에 의존하는 서비스는 **레플리카를 한 번에 하나씩** 붙이는 식으로 운영

---

## 3부. 케이스별 비교

### 마스터 측 전송 방식

| 축 | Case A. 디스크 사용 | Case B. 디스크 미사용 (Diskless) |
|---|---|---|
| **설정** | `repl-diskless-sync no` | `repl-diskless-sync yes` (7.0+ 기본) |
| **RDB 경로** | 메모리 → **디스크 파일** → 소켓 | 메모리 → **소켓 직행** |
| **마스터 디스크 I/O** | 쓰기 + 읽기 발생 | **없음** |
| **파일 흔적** | `dump.rdb` 남음 (영속화 off여도) | 없음 |
| **여러 레플리카** | **파일/BGSAVE 재사용** 가능 | delay 동안 모인 것만 **한 번에**, 이후 합류는 다음 전송 |
| **시작 지연** | 즉시 | `repl-diskless-sync-delay` (기본 5초) |
| **전송 중 끊김** | 파일은 남아 있어 **재전송 용이** | 처음부터 재시작 |
| **유리한 환경** | 디스크 빠름, 레플리카 시차 합류 많음 | **디스크 느림 / 네트워크 빠름**, 동시 다수 합류 |

### 레플리카 측 로딩 방식

| 축 | disabled | on-empty-db | swapdb |
|---|---|---|---|
| **디스크 사용** | 임시 파일 사용 | 비면 미사용 | 미사용 |
| **로딩 중 구 데이터** | 먼저 flush (서비스 불가) | 비면 해당 없음 | **유지하며 서비스 가능** |
| **메모리** | 보통 | 보통 | **약 2배 필요** |
| **실패 시** | 파일 재시도 가능 | — | **롤백** (구 데이터 유지) |

### 조합 요약

| 마스터 | 레플리카 | 결과 |
|---|---|---|
| 디스크 | 디스크 | 가장 전통적. 안정적이지만 I/O 많고 느릴 수 있음 |
| Diskless | 디스크 (**기본 조합**) | 마스터 I/O 절감, 레플리카는 파일로 안전하게 수신 |
| Diskless | swapdb | **완전 무디스크**. 빠르지만 레플리카 메모리 2배·끊김 시 처음부터 |

---

## 4부. 지속적 복제 — 비동기 명령 전파

전체 동기화가 끝나면 마스터는 **실행한 쓰기 명령을 레플리카에 계속 전달**한다. 이 단계가 **비동기**라는 점이 복제의 가장 중요한 특성이다.

### 전파 흐름

```
Client ──SET k v──▶ Master
                     │ 1. 명령 실행 (메모리 갱신)
                     │ 2. 클라이언트에 OK 응답   ◀── 여기서 이미 응답 완료
                     │ 3. 복제 버퍼/backlog에 기록
                     ▼
                  (비동기) 레플리카 소켓으로 전송 ──▶ Replica가 같은 명령 실행
                                                    ▲
                                       REPLCONF ACK <offset> (1초마다 보고)
```

- **마스터는 레플리카의 적용을 기다리지 않고** 클라이언트에 응답 → **복제 지연(lag)이 존재**
- 레플리카는 **1초마다 `REPLCONF ACK <offset>`** 으로 자신이 처리한 위치를 마스터에 보고
- 마스터는 이를 기반으로 레플리카별 **`lag`**(마지막 ACK 이후 경과 초)과 **오프셋 차이**를 관리 → `INFO replication`의 `slaveN:...offset=...,lag=...`

### 전파되는 명령의 가공

- 그대로 전달이 원칙이지만 **결정적(deterministic) 결과를 보장**하도록 일부는 변환
  - `EXPIRE` 류 → 절대 시각(`PEXPIREAT`)로 변환
  - 키 만료 → 마스터가 **`DEL`을 복제 스트림에 넣어 전파** (레플리카는 **스스로 만료 삭제하지 않음**)
  - `SPOP` 같은 비결정적 명령 → `SREM` 등으로 변환
- 레플리카는 **읽기 전용**(`replica-read-only yes`)이며 마스터가 보낸 명령만 적용

### 복제 버퍼 (마스터 측)

- 마스터는 각 레플리카에 보낼 데이터를 **출력 버퍼**에 쌓아 전송
- **7.0+**: 레플리카별 개별 버퍼 대신 **공유 복제 버퍼(shared replication buffer)** 를 사용하고 backlog도 이를 참조 → 레플리카가 많아도 **메모리 중복 감소**
- 레플리카가 느려 버퍼가 한계를 넘으면 **연결 강제 종료**:

  ✅ **기본값 확인**: `client-output-buffer-limit replica 256mb 64mb 60`
  - **하드 256MB 초과** 또는 **소프트 64MB를 60초 이상 유지**하면 끊김
  - 끊기면 다시 붙어 **전체 동기화**를 하게 되고, 그 사이에 또 쌓이면 **무한 재동기화 루프**에 빠질 수 있음 (큰 데이터셋 + 쓰기 폭주 환경에서 흔한 장애)

### 비동기 방식의 특성

| 특성 | 의미 |
|---|---|
| **응답 속도** | 마스터 쓰기 지연에 복제가 영향을 주지 않음 (빠름) |
| **복제 지연** | 레플리카가 마스터보다 **뒤처질 수 있음** → 읽기 시 **stale 데이터** 가능 |
| **내구성** | 마스터가 죽는 순간 **아직 전송되지 않은 쓰기는 유실** |
| **일관성** | **최종 일관성**. 강한 일관성이 필요한 읽기는 마스터에서 |

---

## 5부. 비동기의 한계와 보완 (WAIT, min-replicas)

### 문제 — failover 시 데이터 유실

```
t0  Client: SET order:1 paid   → Master가 OK 응답
t1  Master 장애 (레플리카로 아직 미전파)
t2  Replica가 새 마스터로 승격 → order:1 없음 (유실)
```

- **응답을 받은 쓰기도 유실될 수 있다.** 비동기 복제의 본질적 한계
- 파티션 상황에서 구 마스터가 계속 쓰기를 받다 새 마스터에 밀리면, 구 마스터의 쓰기는 **버려짐**

### 보완 1. `WAIT numreplicas timeout` — 동기화 흉내

```
SET order:1 paid
WAIT 1 100        # 레플리카 1대 이상이 ACK 할 때까지 최대 100ms 대기 → 확인된 레플리카 수 반환
```

✅ **검증**: 레플리카 1대 구성에서 `WAIT 1 100` → `1` 반환

- 현재 클라이언트의 **이전 쓰기들이 지정 수의 레플리카에 도달**할 때까지 블록
- **완전한 동기 복제가 아님**: 반환값이 요청 수보다 작으면 **전파 실패를 알 뿐, 쓰기를 롤백해주지는 않음**
- 레플리카가 **메모리에만** 받은 것이지 디스크에 영속화됐다는 뜻은 아님 → 7.2+ 의 **`WAITAOF`** 로 AOF fsync까지 확인 가능
- 매번 쓰면 **지연 증가** → 중요한 쓰기에만 선택적 사용

### 보완 2. 쓰기 자체를 제한 — `min-replicas-to-write` / `min-replicas-max-lag`

```
min-replicas-to-write 1     # 정상(lag 이내) 레플리카가 1대 미만이면 쓰기 거부
min-replicas-max-lag 10     # lag이 10초 이내인 레플리카만 "정상"으로 간주
```

✅ 기본값: `min-replicas-to-write 0`(비활성), `min-replicas-max-lag 10`

- 레플리카가 부족하면 마스터가 **`-NOREPLICAS` 에러로 쓰기를 거부**
- **고립된 구 마스터가 계속 쓰기를 받아 유실**되는 상황을 **제한**(완전 방지는 아님)
- 대가: **가용성 감소** (레플리카 장애가 곧 쓰기 중단)

### 비동기 복제 보완 비교

| 수단 | 방지하는 것 | 대가 |
|---|---|---|
| 아무것도 안 함 | — | 유실 감수, 최고 성능 |
| `WAIT` | 특정 쓰기의 전파 확인 | 호출당 지연, 롤백 불가 |
| `WAITAOF` (7.2+) | 전파 + 레플리카 AOF fsync 확인 | 지연 더 큼 |
| `min-replicas-*` | 고립된 마스터의 계속 쓰기 | 쓰기 가용성 저하 |
| 클러스터/센티널 | failover 자동화 (유실은 여전히 가능) | 운영 복잡도 |

---

## 6부. 부분 재동기화 (Partial Resync)

네트워크 순단 등으로 **잠시 끊겼다 재연결**할 때 **전체 동기화 없이 끊긴 지점부터 이어받는** 기능.

### 동작

```
1. 레플리카: PSYNC <master replid> <내가 받은 offset+1>
2. 마스터:  replid 일치 AND 요청 offset이 backlog 범위 안?
              ├─ YES → +CONTINUE → backlog에서 끊긴 지점부터 전송
              └─ NO  → +FULLRESYNC → 전체 동기화 (2부 방식)
```

✅ **검증**: `CLIENT KILL TYPE replica` 로 연결을 끊은 뒤

```
[replica] Trying a partial resynchronization (request ...)
[replica] Successful partial resynchronization with master.
[master]  Partial resynchronization request from 127.0.0.1:... accepted.
INFO stats → sync_full:1  sync_partial_ok:1  sync_partial_err:0
```

### Replication Backlog

- 마스터가 최근 복제 스트림을 보관하는 **고정 크기 링 버퍼**
- ✅ 기본값: `repl-backlog-size 1mb` (1048576), `repl-backlog-ttl 3600`
- **끊긴 동안 쌓인 쓰기량 > backlog 크기**이면 오프셋이 이미 덮어써져 **부분 재동기화 불가 → 전체 동기화**
- 쓰기 많은 환경에서는 **수십~수백 MB** 로 상향 (끊김 허용 시간 × 초당 쓰기량으로 산정)
- `repl-backlog-ttl`: 레플리카가 **모두 끊긴 뒤** backlog를 유지하는 시간(초)

### PSYNC2 (4.0+) — failover 후에도 부분 재동기화

- 레플리카가 새 마스터로 승격되면 replid가 바뀜 → 과거에는 모든 레플리카가 **전체 동기화**
- PSYNC2는 **이전 replid(`replid2`)와 그때까지의 오프셋**을 기억해, 같은 이력이면 **부분 재동기화** 허용
- 덕분에 센티널/클러스터 failover 직후 **대규모 전체 동기화 폭풍**을 피함
- 레플리카 **재시작 시에도** RDB에 replid/offset을 저장해 부분 재동기화 가능 (재시작 후 `SHUTDOWN` 시 저장되는 정보 기반)

### 전체 동기화로 넘어가는 경우

| 상황 | 이유 |
|---|---|
| 최초 연결 | replid/offset 없음 |
| 끊긴 시간 동안 backlog 초과 | 이어받을 데이터가 사라짐 |
| 마스터 교체 후 이력 불일치 | replid 불일치 (PSYNC2로 완화) |
| 레플리카 출력 버퍼 한도 초과로 끊김 | 대량 지연 후 backlog 범위 이탈 |
| **영속화 없는 마스터가 재시작** | 데이터·replid 소실 → 레플리카는 전체 동기화 후 **빈 데이터로 덮임** (7장 마지막 체크리스트 참고) |

---

## 7부. 설정 정리

| 설정 | 기본값 (✅ 7.2.6 확인) | 설명 |
|---|---|---|
| `replicaof` | 없음 | 복제 대상 마스터 |
| `masterauth` / `masteruser` | 없음 | 마스터 인증 정보 (마스터가 `requirepass`/ACL일 때 필수) |
| `replica-read-only` | `yes` | 레플리카 쓰기 금지 |
| `replica-serve-stale-data` | `yes` | 마스터와 끊겨도(또는 동기화 중에도) 읽기 응답. `no`면 `-MASTERDOWN` |
| `replica-priority` | `100` | 센티널 승격 우선순위 (0 = 승격 제외) |
| `repl-diskless-sync` | `yes` | 마스터 diskless 전송 |
| `repl-diskless-sync-delay` | `5` | diskless 시작 전 대기(초) |
| `repl-diskless-sync-max-replicas` | `0` | 이 수만큼 모이면 delay 전 시작 (0 = 비활성) |
| `repl-diskless-load` | `disabled` | 레플리카 로드 방식 (`on-empty-db`/`swapdb`) |
| `rdb-del-sync-files` | `no` | 영속화 꺼진 인스턴스에서 동기화용 RDB 삭제 |
| `repl-backlog-size` | `1mb` | 부분 재동기화 버퍼 |
| `repl-backlog-ttl` | `3600` | 레플리카가 없을 때 backlog 유지(초) |
| `repl-timeout` | `60` | 복제 I/O 타임아웃 |
| `repl-ping-replica-period` | `10` | 마스터 → 레플리카 PING 주기(초) |
| `client-output-buffer-limit replica` | `256mb 64mb 60` | 레플리카 출력 버퍼 한도 |
| `min-replicas-to-write` | `0` | 정상 레플리카가 이보다 적으면 쓰기 거부 |
| `min-replicas-max-lag` | `10` | "정상" 레플리카 판정 lag(초) |
| `replica-lazy-flush` | `no` | 전체 동기화 시 기존 데이터 **비동기 삭제** (`yes` 권장) |

### 모니터링

```
INFO replication
# role:master
# connected_slaves:1
# slave0:ip=...,port=...,state=online,offset=...,lag=0     ← offset 차이 / lag
# master_repl_offset:...
# repl_backlog_active:1  repl_backlog_size:1048576

# 레플리카 쪽
# master_link_status:up
# master_last_io_seconds_ago:...
# master_sync_in_progress:0
# slave_repl_offset:...

INFO stats
# sync_full / sync_partial_ok / sync_partial_err        ← 전체 동기화 빈도 점검
```

---

## 8부. 운영 이슈와 체크리스트

### 흔한 장애 패턴

1. **전체 동기화 폭풍**: 마스터 재시작/failover 시 레플리카 다수가 동시에 전체 동기화 → 마스터 **fork + 네트워크 + 메모리(COW)** 급증
   - 대응: PSYNC2 활용, **backlog 상향**, 레플리카를 **시차를 두고** 붙이기, diskless delay 활용
2. **출력 버퍼 초과 → 재동기화 루프**: 쓰기 폭주 + 느린 레플리카 → 끊김 → 전체 동기화 → 다시 쌓임
   - 대응: `client-output-buffer-limit replica` 상향, 동기화 시간 단축(diskless/네트워크), 쓰기 완화
3. **영속화 없는 마스터의 자동 재시작**: 빈 마스터가 올라와 레플리카가 **빈 데이터로 덮어씀**
   - 대응: 마스터 **영속화 유지**, 자동 재시작 금지(센티널/클러스터가 승격 담당)
4. **큰 데이터셋의 fork 지연**: 전체 동기화마다 fork → 메인 스레드 정지
   - 대응: 인스턴스 크기 제한(수~수십 GB), THP 끄기
5. **레플리카 읽기의 stale 데이터**: 복제 지연 중 읽기
   - 대응: 일관성이 필요한 읽기는 마스터, `WAIT`, lag 모니터링

### 체크리스트

- [ ] 마스터가 **인증/ACL**이면 레플리카에 `masterauth`(+`masteruser`) 설정
- [ ] `repl-backlog-size` 를 **끊김 허용 시간 × 초당 쓰기 바이트**로 산정해 상향
- [ ] 디스크가 느리거나 AOF 경합이 있으면 **diskless**, 레플리카 여러 대면 `repl-diskless-sync-delay` 조정
- [ ] 레플리카 메모리 여유가 충분하면 `repl-diskless-load swapdb` 검토 (아니면 기본 유지)
- [ ] `replica-lazy-flush yes` — 전체 동기화 시 구 데이터 삭제 지연 방지
- [ ] 중요 쓰기에 `WAIT`/`WAITAOF`, 고립 마스터 방지에 `min-replicas-*` 검토 (가용성 대가 인지)
- [ ] `sync_full` 증가율, `lag`, `master_link_status` **모니터링/알람**
- [ ] 마스터 **영속화 유지** + 자동 재시작 정책 점검

---

### 요약

- **복제 3단계**: 핸드셰이크/`PSYNC` → **초기 동기화**(Full 또는 Partial) → **지속 복제**(명령 전파). 식별은 **replid + offset**
- **전체 동기화 — 디스크 사용**(`repl-diskless-sync no`): fork → **마스터 디스크에 RDB 저장** → 전송. 파일/BGSAVE 재사용으로 시차 합류에 유리, 대신 마스터 디스크 I/O. ✅ 영속화 꺼도 `dump.rdb` 생성
- **전체 동기화 — 디스크 미사용**(Diskless, 7.0+ 기본): 자식이 **RDB를 소켓으로 직행**. 마스터 I/O 없음, `delay`(기본 5초) 동안 합류한 레플리카에 한 번에 전송. 끊기면 처음부터. 레플리카는 `repl-diskless-load`(disabled/on-empty-db/**swapdb**)로 로드 방식 별도 선택, swapdb는 **완전 무디스크**지만 메모리 2배
- **지속 복제는 비동기**: 마스터는 **응답 후 전파**, 레플리카는 1초마다 `REPLCONF ACK`. lag 존재, **failover 시 응답받은 쓰기도 유실 가능**. 레플리카는 스스로 만료 삭제하지 않고 마스터의 `DEL`을 받음
- **비동기 보완**: `WAIT`(전파 확인, 롤백 불가) / `WAITAOF`(7.2+) / `min-replicas-to-write` (고립 마스터 쓰기 제한, 가용성 대가)
- **부분 재동기화**: 끊겼다 복구 시 **backlog**(기본 1MB)에서 이어받기. backlog 초과면 전체 동기화. **PSYNC2**로 failover 후에도 부분 재동기화
- **운영 포인트**: 전체 동기화 폭풍·출력 버퍼 초과 루프·**영속화 없는 마스터 자동 재시작 사고**를 경계. `backlog`·`output buffer`·`delay` 튜닝과 `sync_full`/`lag` 모니터링
