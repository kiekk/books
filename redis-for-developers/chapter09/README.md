### CHAPTER 9 센티널

8장의 복제는 **데이터를 복사**해 줄 뿐, 마스터가 죽었을 때 **누가 새 마스터가 될지 정하고 클라이언트에게 알리는 일**은 하지 않는다. **Sentinel(센티널)** 이 이 공백을 메운다. 마스터를 **감시**하고, 죽으면 레플리카 중 하나를 **자동 승격(failover)** 시키며, 클라이언트가 **현재 마스터 주소를 물어볼 수 있는 창구**가 된다. 핵심 키워드는 **SDOWN / ODOWN / quorum / majority / epoch / leader 선출 / replica 선택 기준**이다.

> 이 장의 동작은 **로컬 Redis 7.2.6에서 마스터 1 + 레플리카 2 + 센티널 3(quorum 2)을 직접 띄워 failover를 일으켜** 로그와 명령 결과로 확인했다. 확인한 부분은 `✅ 검증` 표시.

#### 목차

- [1부. 센티널 개요와 특징](#1부-센티널-개요와-특징)
- [2부. 구성과 자동 발견](#2부-구성과-자동-발견)
- [3부. 장애 감지 — SDOWN vs ODOWN, quorum](#3부-장애-감지--sdown-vs-odown-quorum)
- [4부. Failover 동작 방식](#4부-failover-동작-방식)
- [5부. 승격 대상(레플리카) 선택 기준](#5부-승격-대상레플리카-선택-기준)
- [6부. 주요 설정 지시자](#6부-주요-설정-지시자)
- [7부. 주요 명령어](#7부-주요-명령어)
- [8부. 이벤트(Pub/Sub) 키워드](#8부-이벤트pubsub-키워드)
- [9부. 클라이언트 연동](#9부-클라이언트-연동)
- [10부. 한계와 운영 주의사항](#10부-한계와-운영-주의사항)
- [11부. Sentinel vs Cluster](#11부-sentinel-vs-cluster)

---

## 1부. 센티널 개요와 특징

### 센티널의 4가지 역할

| 역할 | 설명 |
|---|---|
| **Monitoring (감시)** | 마스터·레플리카·다른 센티널이 정상인지 **주기적으로 확인** |
| **Notification (알림)** | 장애/전환 이벤트를 Pub/Sub 및 **스크립트**로 통지 |
| **Automatic Failover (자동 장애 조치)** | 마스터 장애 시 레플리카를 **새 마스터로 승격**하고 나머지 레플리카를 재지정 |
| **Configuration Provider (설정 제공)** | 클라이언트가 **"지금 마스터가 누구냐"** 를 센티널에게 질의 |

### 핵심 특징

- **별도 프로세스**: 데이터 노드가 아니라 **`redis-sentinel`(= `redis-server --sentinel`)** 로 띄우는 전용 프로세스. 기본 포트 **26379**. 데이터를 저장하지 않음
- **분산 합의**: 센티널 **여러 대가 투표**해서 장애를 판단하고 failover 리더를 뽑음 → 센티널 자체가 단일 장애점이 되지 않음
- **최소 3대, 홀수 권장**: 과반수(majority)를 확보하려면 3대 이상. **서로 다른 장애 도메인(호스트/AZ)** 에 분산
- **프록시가 아니다**: 트래픽을 중계하지 않음. 클라이언트가 센티널에게 **주소를 물어본 뒤 마스터에 직접 접속**
- **Replica 기반 HA**: 8장의 비동기 복제 위에서 동작 → **failover 시 일부 쓰기 유실 가능**
- **자동 발견**: 마스터만 알려주면 레플리카와 다른 센티널을 **스스로 찾음** (2부)
- **설정 자동 갱신**: 상태 변화를 **`sentinel.conf`에 직접 다시 씀** → 파일이 **쓰기 가능**해야 함
- **샤딩 없음**: 마스터 1대가 전체 데이터를 담당 → **쓰기 확장 불가** (확장은 Cluster)

---

## 2부. 구성과 자동 발견

### 최소 구성

```
              ┌────────────┐  ┌────────────┐  ┌────────────┐
              │ Sentinel 1 │  │ Sentinel 2 │  │ Sentinel 3 │   ← 서로 다른 호스트
              └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
                    │ 감시/INFO/PING │               │
          ┌─────────┴───────────────┴───────────────┴─────────┐
          ▼                         ▼                          ▼
     ┌─────────┐   복제 ──▶   ┌──────────┐   복제 ──▶   ┌──────────┐
     │ Master  │              │ Replica 1│              │ Replica 2│
     └─────────┘              └──────────┘              └──────────┘
```

### 기동

```bash
redis-sentinel /path/sentinel.conf
# 또는
redis-server /path/sentinel.conf --sentinel
```

```
# sentinel.conf — 최소 설정 (검증에 사용한 설정)
port 26379
sentinel monitor mymaster 127.0.0.1 6379 2      # 이름, IP, 포트, quorum
sentinel down-after-milliseconds mymaster 3000
sentinel failover-timeout mymaster 10000
sentinel parallel-syncs mymaster 1
```

- **레플리카·센티널 주소는 적지 않는다** — 마스터만 지정

### 자동 발견 메커니즘

| 대상 | 발견 방법 |
|---|---|
| **레플리카** | 센티널이 마스터에 주기적으로(기본 10초) **`INFO`** 를 보내 응답의 `slaveN:` 목록으로 파악 |
| **다른 센티널** | 마스터/레플리카의 Pub/Sub 채널 **`__sentinel__:hello`** 에 **2초마다 자신의 정보를 publish** → 같은 채널을 구독한 센티널들이 서로 인식 |

✅ **검증**: 센티널에 마스터만 등록했는데 `SENTINEL master mymaster` 결과가 `num-slaves 2`, `num-other-sentinels 2`, `INFO sentinel` 이 `slaves=2,sentinels=3`

### 센티널이 `sentinel.conf`에 스스로 쓰는 것

✅ **검증** — 기동 후 파일에 자동 추가된 항목:

```
sentinel myid 0891608030b4...                         ← 센티널 고유 ID
sentinel config-epoch mymaster 0
sentinel leader-epoch mymaster 0
sentinel current-epoch 0
sentinel known-replica mymaster 127.0.0.1 7002        ← 발견한 레플리카
sentinel known-replica mymaster 127.0.0.1 7003
sentinel known-sentinel mymaster 127.0.0.1 27002 <id> ← 발견한 센티널
sentinel known-sentinel mymaster 127.0.0.1 27003 <id>
```

- 재시작해도 이 정보로 **바로 이어서 동작**. 그래서 설정 파일이 **읽기 전용이면 안 됨**
- Docker/설정관리 도구로 **매번 원본 설정을 덮어쓰면** 학습한 상태(epoch, known-*)가 사라지니 주의

---

## 3부. 장애 감지 — SDOWN vs ODOWN, quorum

### 두 단계 감지

```
[SDOWN] Subjectively Down — "내 눈에는" 죽었다 (센티널 1대의 주관적 판단)
            │  다른 센티널들에게 확인 요청 (SENTINEL is-master-down-by-addr)
            ▼
[ODOWN] Objectively Down — quorum 이상의 센티널이 "죽었다"에 동의 (객관적 판단)
            │
            ▼
        Failover 시작 조건 충족
```

| 상태 | 의미 | 조건 |
|---|---|---|
| **SDOWN** | 한 센티널이 보기에 장애 | `down-after-milliseconds` 동안 **유효한 PING 응답이 없음** (또는 비정상 응답) |
| **ODOWN** | 합의된 장애 | **quorum 수 이상**의 센티널이 SDOWN으로 판단 (마스터에만 적용) |

- **ODOWN은 마스터에만** 존재. 레플리카/센티널은 SDOWN까지만
- 센티널은 마스터·레플리카·센티널 모두에게 **1초마다 PING** 을 보냄

✅ **검증** (`down-after-milliseconds 3000`, 마스터 종료 직후)

```
07:52:52  마스터 종료
07:52:55  +sdown master mymaster 127.0.0.1 7001           ← 약 3초 후 (down-after)
07:52:55  +odown master mymaster 127.0.0.1 7001 #quorum 2/2   ← quorum 2/2 도달
```

### quorum vs majority — 가장 혼동하기 쉬운 개념

| | quorum | majority(과반) |
|---|---|---|
| **용도** | **ODOWN 판정**에 필요한 동의 수 | **failover 실행 권한(리더 선출)** 에 필요한 투표 수 |
| **값** | 설정으로 지정 (`monitor ... <quorum>`) | **센티널 전체 수의 과반** (자동 계산) |
| **역할** | "정말 죽었는가?" | "누가 failover를 책임질 것인가?" |

- **quorum은 감지에만** 쓰인다. 장애가 확정(ODOWN)돼도 **과반수 센티널의 승인이 없으면 failover는 시작되지 못함**
- 리더 선출에는 **과반수 투표**가 필요하며, **quorum이 과반보다 크면 quorum 수만큼의 투표**가 필요

| 센티널 수 | quorum | 과반 | 결과 |
|---|---|---|---|
| 3 | 2 | 2 | ✅ 정상. 1대가 죽어도 failover 가능 |
| 3 | 3 | 3 | ⚠️ 1대만 죽어도 ODOWN/리더 선출 불가 → **failover 불가** |
| 5 | 2 | 3 | 감지는 쉽지만 **리더 선출은 3대 필요** |
| 5 | 3 | 3 | ✅ 일반적. 2대까지 장애 허용 |
| 2 | 2 | 2 | ❌ 1대 장애 시 과반 불가 → **2대 구성은 무의미** |

✅ **검증**: `SENTINEL ckquorum mymaster` →
`OK 3 usable Sentinels. Quorum and failover authorization can be reached`
(→ quorum 도달과 **failover 승인(과반)** 둘 다 가능한지 점검해 주는 명령)

> **quorum을 낮게(예: 1) 잡으면** 센티널 1대의 오탐(네트워크 단절)만으로 failover가 일어날 수 있고, **너무 높게** 잡으면 센티널이 몇 대 죽었을 때 failover가 불가능. 보통 **과반**으로 설정.

### SDOWN 오탐 요인

- 센티널과 마스터 사이 **네트워크 분리** (마스터는 멀쩡)
- 마스터의 **긴 블로킹**(큰 키 삭제, `KEYS`, fork 지연) → PING 응답 지연
- `down-after-milliseconds`가 너무 짧음 → 일시적 지연에도 SDOWN

---

## 4부. Failover 동작 방식

### 전체 흐름

```
① SDOWN 감지  →  ② ODOWN 합의(quorum)  →  ③ 리더 센티널 선출(과반 투표)
   →  ④ 승격할 레플리카 선택  →  ⑤ 승격(REPLICAOF NO ONE)
   →  ⑥ 나머지 레플리카 재지정  →  ⑦ 설정 전파(switch-master)
   →  ⑧ (복귀 시) 구 마스터를 레플리카로 전환
```

### epoch (에포크) — 분산 합의의 버전 번호

| 이름 | 의미 |
|---|---|
| **current-epoch** | 클러스터(센티널 집합) 전체의 **논리적 시계**. failover/선거 시도마다 증가 |
| **leader-epoch** | 어떤 센티널이 **그 epoch의 리더로 투표받은** 기록. **한 epoch에 한 센티널은 한 번만 투표** |
| **config-epoch** | 특정 마스터 **설정(주소)의 버전**. failover 성공 시 새 값으로 갱신 |

- 여러 센티널이 동시에 failover를 시도해도 **같은 epoch에 과반 득표자는 최대 1명** → 리더 1명만 진행
- 센티널 간 설정이 엇갈리면 **config-epoch가 큰 쪽이 이김 (last failover wins)** → 이 설정이 Pub/Sub으로 퍼져 모든 센티널이 수렴

### 단계별 상세 + ✅ 실제 로그

마스터(7001) 종료 후 센티널 3 로그 (센티널 3이 리더가 됨):

```
07:52:55.443  +sdown master mymaster 127.0.0.1 7001            ① SDOWN
07:52:55.546  +odown master mymaster 127.0.0.1 7001 #quorum 2/2  ② ODOWN
07:52:55.547  +try-failover master mymaster ...                  ③ failover 시도 시작 (epoch+1)
07:52:55.553  +vote-for-leader 73404e59... 1                     ③ 자신에게 투표 (epoch 1)
07:52:55.645  +elected-leader master mymaster ...                ③ 과반 득표 → 리더 확정
07:52:55.645  +failover-state-select-slave                       ④ 레플리카 선택
07:52:55.704  +selected-slave slave 127.0.0.1:7002 ...            ④ 7002 선택
07:52:55.705  +failover-state-send-slaveof-noone                 ⑤ 7002에 REPLICAOF NO ONE 전송
07:52:55.772  +failover-state-wait-promotion                     ⑤ 승격 확인 대기
07:52:56.608  +promoted-slave slave 127.0.0.1:7002 ...           ⑤ 승격 확인
07:52:56.608  +failover-state-reconf-slaves                      ⑥ 나머지 재지정 단계
07:52:56.665  +slave-reconf-sent slave 127.0.0.1:7003 ...        ⑥ 7003에 REPLICAOF 7002 전송
07:52:57.628  +slave-reconf-inprog / +slave-reconf-done          ⑥ 재동기화 진행 → 완료
07:52:57.699  -odown / +failover-end                             failover 종료
07:52:57.700  +switch-master mymaster 127.0.0.1 7001 127.0.0.1 7002   ⑦ 마스터 주소 변경 통지
```

- **마스터 종료 → failover 종료까지 약 5초** (감지 3초 + 선출·승격·재구성 약 2초)
- 다른 센티널(1, 2)은 리더가 아니므로 **`+vote-for-leader` 와 `+switch-master`만** 기록 — 리더가 한 작업의 결과를 설정 전파로 받아 반영
- 사후 확인:

  ```
  SENTINEL get-master-addr-by-name mymaster  →  127.0.0.1 / 7002
  ```

### ⑧ 구 마스터가 돌아오면

✅ **검증** — 종료했던 7001을 다시 기동:

```
기동 직후:       role → master            ← 잠시 동안 스스로를 마스터로 인식 (⚠️ 이중 마스터 구간)
수 초~십수 초 후: role → slave 127.0.0.1 7002   ← 센티널이 REPLICAOF 로 강제 전환
센티널 로그:     -sdown slave 127.0.0.1:7001 ... @ mymaster 127.0.0.1 7002
```

- 센티널은 구 마스터를 **레플리카로 등록해 두고 있다가**, 살아나면 **새 마스터의 레플리카로 재설정**
- 전환되면서 **구 마스터의 데이터는 새 마스터 데이터로 덮임** (failover 이전에 복제되지 못한 쓰기는 **유실**)
- 이중 마스터 구간 동안 클라이언트가 구 마스터에 쓰면 그 쓰기도 사라짐 → 클라이언트는 **항상 센티널을 통해 주소를 얻어야** 함

### 수동 failover

✅ **검증** — `SENTINEL FAILOVER mymaster` → `OK`, 몇 초 뒤 마스터가 7003으로 변경(`config-epoch` 증가)

- **다른 센티널의 합의 없이** 즉시 failover 시작 (ODOWN 불필요)
- 운영 중 **마스터 노드 점검·교체** 시 계획된 전환에 사용

---

## 5부. 승격 대상(레플리카) 선택 기준

리더 센티널은 **다음 순서로 후보를 걸러 하나를 선택**한다.

```
1) 부적격 제외
   - SDOWN / ODOWN 상태이거나 연결이 끊긴 레플리카
   - 최근 INFO 응답이 너무 오래된 레플리카 (≥ 5초)
   - 마스터와 연결이 끊긴 지 너무 오래된 레플리카
       (> down-after-milliseconds × 10 + 마스터가 SDOWN이었던 시간)
   - replica-priority 가 0 인 레플리카  (승격 영구 제외)

2) 남은 후보 정렬
   ① replica-priority 가 낮은 쪽 우선   (기본 100, 숫자가 작을수록 선호)
   ② 같으면 replication offset 이 큰 쪽 우선 (= 마스터 데이터를 더 많이 받은 쪽)
   ③ 같으면 run ID 가 사전순으로 작은 쪽
```

- `replica-priority`는 레플리카 `redis.conf`에서 설정: **0 = 절대 승격 안 함**(분석용·백업용 레플리카에 적합)
- ②번 덕분에 **데이터 유실을 최소화**하는 방향으로 선택
- ✅ 검증: 두 레플리카가 모두 priority 100이고 offset 동일 → run ID 기준으로 7002 선택됨 (`slave-priority 100` 확인)

### 승격 후 재구성 속도 — `parallel-syncs`

- 새 마스터에 **동시에 재동기화(SYNC)할 수 있는 레플리카 수**
- 기본 `1`: 레플리카를 **한 번에 하나씩** 새 마스터에 붙임 → 재동기화 중인 레플리카만 읽기 불가가 되고, 나머지는 계속 읽기를 처리 가능
- 값을 키우면 **전체 재구성이 빨라지지만**, 동시에 여러 레플리카가 재동기화(`LOADING`)에 들어가 **읽기를 받을 레플리카가 한꺼번에 줄고 새 마스터의 전송 부담도 커짐**
- 8장의 전체 동기화 부하와 직결: 레플리카가 많으면 크게 올리지 말 것

---

## 6부. 주요 설정 지시자

| 지시자 | 기본값 | 설명 |
|---|---|---|
| `sentinel monitor <name> <ip> <port> <quorum>` | — | 감시 대상 마스터 등록 + quorum |
| `sentinel down-after-milliseconds <name> <ms>` | `30000` | 이 시간 응답 없으면 **SDOWN**. 짧을수록 빠른 감지/오탐 증가 |
| `sentinel failover-timeout <name> <ms>` | `180000` | failover 각 단계의 **제한 시간**. 같은 마스터에 대한 **재시도 간격(×2)** 기준이기도 함 |
| `sentinel parallel-syncs <name> <n>` | `1` | 승격 후 동시에 재동기화할 레플리카 수 |
| `sentinel auth-pass <name> <password>` | — | 마스터/레플리카 **requirepass** 가 있을 때 |
| `sentinel auth-user <name> <user>` | — | 마스터/레플리카 **ACL 사용자** (6.0+) |
| `requirepass` / `sentinel sentinel-pass` | — | **센티널 자신**에 대한 인증 (센티널 간 통신 포함) |
| `sentinel announce-ip` / `announce-port` | — | **NAT/Docker** 환경에서 외부에 알릴 주소 |
| `sentinel resolve-hostnames yes` | `no` | **호스트명** 사용 허용 (6.2+) |
| `sentinel announce-hostnames yes` | `no` | 센티널이 호스트명으로 알림 |
| `sentinel notification-script <name> <path>` | — | **이벤트 발생 시** 실행 (알림 연동) |
| `sentinel client-reconfig-script <name> <path>` | — | **failover 후** 실행 (VIP 변경 등 외부 연동) |
| `sentinel deny-scripts-reconfig yes` | `yes` | 런타임에 스크립트 경로 변경 금지 (보안) |

> `down-after-milliseconds` 기본이 **30초**라 기본값 그대로면 장애 감지만 30초가 걸린다. 서비스 요구에 맞게 **수 초 ~ 10초대**로 조정하되 오탐과 균형을 맞출 것. (검증에서는 3초 사용)

---

## 7부. 주요 명령어

센티널에 접속해(`redis-cli -p 26379`) 사용. 일반 Redis 명령(`GET` 등)은 대부분 지원하지 않고 **`SENTINEL` 하위 명령**을 쓴다.

### 조회

| 명령 | 설명 |
|---|---|
| `SENTINEL masters` | 감시 중인 **모든 마스터**의 상태 |
| `SENTINEL master <name>` | 특정 마스터 상세 (`flags`, `num-slaves`, `num-other-sentinels`, `quorum`, `config-epoch`, `failover-state` …) |
| `SENTINEL replicas <name>` | 레플리카 목록·상태 (`master-link-status`, `slave-priority`, `slave-repl-offset`) — 구버전 `SENTINEL slaves` |
| `SENTINEL sentinels <name>` | 다른 센티널 목록 |
| `SENTINEL get-master-addr-by-name <name>` | **현재 마스터 IP/포트** (클라이언트가 사용) |
| `SENTINEL myid` | 이 센티널의 ID |
| `SENTINEL ckquorum <name>` | **quorum 도달 + failover 승인(과반)** 가능 여부 점검 |
| `SENTINEL info-cache <name>` | 마스터·레플리카의 **캐시된 INFO** |
| `SENTINEL pending-scripts` | 대기 중인 알림/재설정 스크립트 |
| `SENTINEL is-master-down-by-addr <ip> <port> <epoch> <runid>` | **센티널 간 내부 통신**: 마스터 다운 여부 질의 + 리더 투표 |
| `INFO sentinel` | 요약 (`master0:name=...,status=ok,address=...,slaves=2,sentinels=3`) |

### 제어

| 명령 | 설명 |
|---|---|
| `SENTINEL failover <name>` | **수동 failover** (합의 없이 즉시 시작) |
| `SENTINEL monitor <name> <ip> <port> <quorum>` | 감시 **추가** (런타임) |
| `SENTINEL remove <name>` | 감시 **제거** |
| `SENTINEL set <name> <option> <value>` | 마스터별 옵션 변경 (`down-after-milliseconds`, `quorum`, `parallel-syncs`, `failover-timeout` …) |
| `SENTINEL config get/set <param>` | **전역** 센티널 설정 조회/변경 (7.0+) |
| `SENTINEL reset <pattern>` | 마스터 상태 **초기화** → 레플리카/센티널 정보를 **다시 발견** (센티널 추가·제거 후 사용) |
| `SENTINEL flushconfig` | 현재 상태를 `sentinel.conf`에 **강제로 기록** |
| `SENTINEL simulate-failure crash-after-election \| crash-after-promotion \| help` | **failover 중 센티널 장애** 시뮬레이션 (테스트용) |

### 운영 팁

- **센티널 추가/교체**: 새 센티널에 같은 `monitor` 설정으로 띄우면 자동 발견 → 죽은 센티널 정보가 남아 있으면 **각 센티널에서 `SENTINEL reset <name>`** 으로 정리 (한 번에 모두 하지 말고 **30초 이상 간격**)
- **센티널 수 변경 시 `ckquorum`** 으로 항상 재확인
- 계획된 점검: 마스터 서버를 끄기 전에 **`SENTINEL FAILOVER`** 로 먼저 전환

---

## 8부. 이벤트(Pub/Sub) 키워드

센티널에 `PSUBSCRIBE *` 하면 상태 변화가 **채널 이름 = 이벤트명**으로 전달된다. 접두 `+`는 발생/진입, `-`는 해제.

| 이벤트 | 의미 |
|---|---|
| `+sdown` / `-sdown` | 인스턴스가 주관적 다운 / 복구 |
| `+odown` / `-odown` | 마스터가 객관적 다운 / 해제 |
| `+try-failover` | failover 시작 시도 |
| `+vote-for-leader <runid> <epoch>` | 해당 epoch 리더 투표 |
| `+elected-leader` | 이 센티널이 failover 리더로 당선 |
| `+failover-state-select-slave` | 승격할 레플리카 선택 단계 |
| `+selected-slave` / `+no-good-slave` | 후보 선택 / **적합한 후보 없음 (failover 중단)** |
| `+failover-state-send-slaveof-noone` | 선택한 레플리카에 승격 명령 전송 |
| `+failover-state-wait-promotion` | 승격 완료 대기 |
| `+promoted-slave` | 레플리카가 마스터로 승격됨 |
| `+failover-state-reconf-slaves` | 나머지 레플리카 재설정 단계 |
| `+slave-reconf-sent / -inprog / -done` | 레플리카 재지정 명령 전송 / 진행 / 완료 |
| `+failover-end` | failover 종료 |
| **`+switch-master <name> <oldip> <oldport> <newip> <newport>`** | **마스터 주소 변경 — 클라이언트가 가장 중요하게 구독** |
| `+slave` / `+sentinel` | 새 레플리카 / 새 센티널 발견 |
| `+tilt` / `-tilt` | **TILT 모드** 진입/해제 |
| `+failover-end-for-timeout` / `-failover-abort-*` | **타임아웃/중단**으로 failover 실패 |

### TILT 모드

- 센티널은 **자신의 타이머가 비정상**(시스템 시계 점프, 프로세스 장시간 블록 등)임을 감지하면 **TILT** 진입
- TILT 동안(기본 **30초**) 센티널은 **감시만 하고 failover·ODOWN 판단·투표를 하지 않음** → **센티널 자신의 문제로 인한 오탐 failover 방지**
- `+tilt` 이벤트가 자주 보이면 **센티널 호스트의 부하·시계**를 점검

---

## 9부. 클라이언트 연동

### 흐름

```
1. 클라이언트: 센티널 목록(예: s1, s2, s3)과 마스터 이름(mymaster)을 설정에 보유
2. 센티널에게 질의: SENTINEL get-master-addr-by-name mymaster → 현재 마스터 IP:PORT
3. 그 주소로 마스터에 직접 접속
4. 센티널의 +switch-master 구독(또는 연결 오류 시 재질의) → 새 마스터로 재접속
```

- 대부분의 클라이언트 라이브러리(**Lettuce, Jedis, Redisson, redis-py, ioredis …**)가 위 과정을 **내장**
- 클라이언트는 **마스터 주소를 하드코딩하지 않는다.** 센티널 주소만 알면 됨
- **읽기 분산**이 필요하면 `SENTINEL replicas`로 레플리카 목록을 받아 읽기 요청을 분배 (라이브러리 옵션: Lettuce `ReadFrom.REPLICA_PREFERRED` 등)

### Spring Boot 설정 예

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD}          # 마스터/레플리카 비밀번호
      sentinel:
        master: mymaster                   # sentinel monitor 이름
        nodes:
          - 10.0.1.11:26379
          - 10.0.2.11:26379
          - 10.0.3.11:26379
        password: ${SENTINEL_PASSWORD}     # 센티널 인증이 있을 때
```

### 클라이언트 주의점

- **failover 중에는 잠시 쓰기 실패/타임아웃**이 발생 → **재시도 로직** 필수 (수 초 ~ 십수 초)
- 연결 풀의 **죽은 커넥션 정리**(검증 쿼리, 짧은 타임아웃) 필요
- Docker/NAT 환경에서 센티널이 **내부 IP를 알려주면** 외부 클라이언트가 접속 불가 → `announce-ip`/`resolve-hostnames`로 해결

---

## 10부. 한계와 운영 주의사항

### 데이터 유실 (비동기 복제의 한계)

- 센티널은 **복제를 동기로 바꾸지 않는다** → failover 시 **마스터에서 응답했지만 레플리카로 못 간 쓰기는 유실**
- 완화: `min-replicas-to-write` / `min-replicas-max-lag` (8장), 중요 쓰기에 `WAIT`

### Split-brain (네트워크 분리)

```
[ 센티널 1 + 구 마스터 ]  ✂  [ 센티널 2, 3 + 레플리카들 ]
        소수파                      과반수파 → 새 마스터 승격
```

- 구 마스터와 **소수 센티널**이 분리되면, 과반수 쪽이 새 마스터를 승격 → **마스터가 두 개** 존재
- 분리 중 구 마스터에 쓴 데이터는 **복구 후 덮여 유실**
- 완화: **`min-replicas-to-write 1`** 로 레플리카가 없는 고립 마스터가 쓰기를 거부하도록 설정 + **센티널을 클라이언트/서버와 같은 쪽에 배치**

### 기타 주의사항

| 항목 | 내용 |
|---|---|
| **센티널 배치** | 3대 이상을 **서로 다른 호스트/AZ**에. 같은 서버 공유 시 서버 1대 장애로 과반 상실 |
| **센티널 수 ≥ 3, 홀수** | 2대는 1대 장애 시 과반 불가 |
| **`down-after-milliseconds`** | 기본 30초는 길다. 단, 너무 짧으면 일시 지연에 **오탐 failover** |
| **영속화** | 마스터 영속화를 끄고 **자동 재시작**하면 빈 마스터가 올라와 레플리카를 덮을 위험 (센티널이 먼저 failover 해주지 못한 경우) |
| **ACL/비밀번호** | 마스터·레플리카에 `requirepass`/ACL이 있으면 센티널에 **`auth-pass`/`auth-user`** 필수, **모든 노드 비밀번호 통일** |
| **쓰기 확장 불가** | 마스터 1대 한계 → 데이터가 커지면 Cluster |
| **설정 파일 쓰기 권한** | 센티널이 설정을 직접 갱신. 읽기 전용 마운트 금지 |
| **failover 테스트** | **운영 투입 전 반드시** 마스터를 내려 failover를 리허설 (`SENTINEL FAILOVER`, `simulate-failure`) |

---

## 11부. Sentinel vs Cluster

| 축 | Sentinel | Cluster |
|---|---|---|
| **목적** | **고가용성(HA)** | **고가용성 + 수평 확장(샤딩)** |
| **데이터 분산** | 없음 (마스터 1대에 전체 데이터) | **16384 슬롯**으로 여러 마스터에 분산 |
| **장애 감지/승격** | **별도 센티널 프로세스**가 수행 | **데이터 노드 자체**가 gossip으로 수행 |
| **쓰기 확장** | ❌ | ✅ |
| **클라이언트** | 센티널에 마스터 주소 질의 | 클러스터 인지 클라이언트 (`MOVED`/`ASK`) |
| **멀티키/트랜잭션** | 제약 없음 | 같은 슬롯만 가능 (해시태그) |
| **운영 복잡도** | 낮음~중간 | 높음 |
| **적합** | 데이터 수~수십 GB, 쓰기 한 대로 충분 | 대용량·높은 쓰기 처리량 |

→ **"HA만 필요하다"면 Sentinel, "용량·쓰기 확장까지 필요하다"면 Cluster**(10장).

---

### 요약

- **센티널**: 마스터 **감시 + 자동 failover + 마스터 주소 제공**을 하는 **별도 프로세스(26379)**. **3대 이상·홀수·서로 다른 장애 도메인**에 배치. **프록시가 아니며** 클라이언트가 주소를 질의해 직접 접속
- **자동 발견**: 마스터만 등록하면 **`INFO`**(레플리카)와 **`__sentinel__:hello` Pub/Sub**(다른 센티널)로 구성원 파악, 결과를 **`sentinel.conf`에 스스로 기록**
- **감지**: **SDOWN**(한 센티널의 주관적 판단, `down-after-milliseconds`) → **ODOWN**(**quorum** 이상 동의, 마스터만). ✅ 설정 3초 → 약 3초 후 `+sdown`, `quorum 2/2` 시 `+odown`
- **quorum vs majority**: quorum은 **감지(ODOWN)** 에만, failover 실행은 **과반수 센티널의 승인(리더 선출)** 필요. `SENTINEL ckquorum`으로 점검
- **failover**: ODOWN → **epoch 증가 + 리더 선출(과반 투표)** → 레플리카 선택 → `REPLICAOF NO ONE` → 나머지 재지정 → **`+switch-master`**. ✅ 마스터 종료 후 약 5초에 완료, 구 마스터는 복귀 시 센티널이 **레플리카로 강제 전환**
- **승격 후보 선택**: 부적격 제외(`replica-priority 0` 등) → **priority 낮은 순 → replication offset 큰 순 → run ID 작은 순**
- **핵심 설정**: `monitor`(quorum), `down-after-milliseconds`(기본 30s), `failover-timeout`, `parallel-syncs`, `auth-pass/auth-user`, `announce-ip`, `notification/client-reconfig-script`
- **핵심 명령**: `SENTINEL masters/master/replicas/sentinels/get-master-addr-by-name/ckquorum/failover/monitor/remove/set/reset/flushconfig/simulate-failure`, `INFO sentinel`
- **한계**: 비동기 복제로 **쓰기 유실 가능**, **split-brain**(→ `min-replicas-to-write`), **쓰기 확장 불가**(→ Cluster). **TILT 모드**는 센티널 자신의 시간 이상 시 오탐 failover를 막는 안전장치
