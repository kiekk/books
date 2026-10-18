### CHAPTER 10 클러스터

9장의 센티널은 **가용성(HA)** 은 해결하지만 마스터가 1대라 **쓰기·메모리 확장은 불가능**하다. **Redis Cluster** 는 데이터를 **16384개 해시 슬롯**으로 쪼개 여러 마스터에 **샤딩**하고, 각 마스터에 레플리카를 두어 **자동 failover** 까지 **하나의 시스템(별도 센티널 없이)** 으로 해결한다. 핵심 키워드는 **hash slot / hash tag / cluster bus(gossip) / MOVED·ASK / PFAIL·FAIL / failover election / epoch / resharding** 이다.

> 이 장의 동작은 **로컬 Redis 7.2.6에서 마스터 3 + 레플리카 3(+ 빈 노드 1)** 클러스터(`cluster-node-timeout 3000`)를 직접 띄워 확인했다. 확인한 부분은 `✅ 검증` 표시. 책/공식 문서 지식이지만 로컬에서 재현하지 않은 수치는 `(문서 기준)` 으로 표시했다.

#### 목차

- [1부. 클러스터 동작 방식](#1부-클러스터-동작-방식)
- [2부. 클라이언트 라우팅 — MOVED vs ASK](#2부-클라이언트-라우팅--moved-vs-ask)
- [3부. 자동 Failover — 장애 감지부터 리더(마스터) 재선출까지](#3부-자동-failover--장애-감지부터-리더마스터-재선출까지)
- [4부. 복제본 선출 — 자격 조건, 우선순위, 처리 방법](#4부-복제본-선출--자격-조건-우선순위-처리-방법)
- [5부. 수동 Failover — CLUSTER FAILOVER 3종](#5부-수동-failover--cluster-failover-3종)
- [6부. 리샤딩(Resharding) 절차와 발생 가능한 이슈](#6부-리샤딩resharding-절차와-발생-가능한-이슈)
- [7부. CLUSTER FORGET / 노드 제거 시 이슈](#7부-cluster-forget--노드-제거-시-이슈)
- [8부. 주요 설정과 명령어 요약](#8부-주요-설정과-명령어-요약)
- [9부. 센티널 vs 클러스터, 한계](#9부-센티널-vs-클러스터-한계)
- [10부. 운영 심화 — 구축·네트워크·클라이언트·튜닝](#10부-운영-심화--구축네트워크클라이언트튜닝)

---

## 1부. 클러스터 동작 방식

### 1-1. 해시 슬롯 (16384 slots)

```
slot = CRC16(key) mod 16384
```

- 키 → 슬롯 → **슬롯을 가진 마스터** 로 라우팅. **노드 수가 아니라 슬롯 단위** 로 분배하므로, 노드를 늘리거나 줄일 때 **슬롯만 옮기면 된다**(전체 재해싱 없음)
- 3마스터 기본 분배 ✅ 검증: `7001: 0-5460`, `7002: 5461-10922`, `7003: 10923-16383`
- 각 마스터는 **자기 슬롯 소유 정보를 비트맵(16384 bit = 2KB)** 으로 가지고 gossip 메시지에 실어 보낸다 (→ 슬롯 수를 16384로 둔 이유: 메시지 크기·노드 수 상한(권장 ~1000)의 균형)

### 1-2. 해시 태그 (Hash Tag)

키에 `{...}` 가 있으면 **중괄호 안 문자열만** 해시한다 → **여러 키를 같은 슬롯에 강제로 모을 수 있다**.

```
CLUSTER KEYSLOT foo           → 12182
CLUSTER KEYSLOT {user:1}:a    → 10778
CLUSTER KEYSLOT {user:1}:b    → 10778   ← 같은 슬롯 ✅ 검증
```

| 상황 | 결과 |
|---|---|
| `MSET a 1 b 2` (슬롯이 다른 키) | `CROSSSLOT Keys in request don't hash to the same slot` ✅ 검증 |
| `MSET {u}:a 1 {u}:b 2` | 같은 슬롯이면 허용 (슬롯 담당 노드가 아니면 `MOVED`) ✅ 검증 |

- 멀티 키 명령(`MGET`/`MSET`/`SUNION`…), **트랜잭션(MULTI/EXEC)**, **Lua 스크립트의 KEYS** 는 모두 **같은 슬롯** 이어야 한다
- 주의: 해시 태그를 과하게 쓰면 **특정 슬롯(노드)에 데이터·트래픽이 쏠림(hot slot)** → 태그 카디널리티를 충분히 크게

### 1-3. 클러스터 버스(Cluster Bus)와 Gossip

- 데이터 포트(6379) + **10000 = 16379** 같은 **버스 포트**(✅ `7001@17001`)로 노드끼리 **바이너리 프로토콜** 통신. 방화벽/보안그룹에서 **양쪽 포트 모두 허용** 필요
- **Gossip**: 각 노드가 주기적으로 일부 노드에 **PING** 을 보내고(자기 상태 + 다른 노드 몇 개의 정보 piggyback) **PONG** 으로 응답 → 전체 노드가 **결국 같은 시야(eventual consistency)** 를 가짐
- 전달 내용: 슬롯 소유, 마스터/레플리카 관계, **PFAIL/FAIL 플래그**, **currentEpoch / configEpoch**
- 별도의 중앙 코디네이터가 없다 → **전 노드가 P2P** (센티널 같은 감시 프로세스 불필요)

### 1-4. 읽기/쓰기 규칙과 레플리카

- 쓰기는 **슬롯 소유 마스터** 로만 간다
- 레플리카는 기본적으로 **슬롯 소유자가 아니므로 `MOVED`** 로 마스터를 안내한다 ✅ 검증 (7004에 `GET foo` → `MOVED 12182 127.0.0.1:7003`)
- 레플리카에서 읽으려면 **`READONLY`** (연결 단위) 를 보내야 하고, 복제는 **비동기** 이므로 **stale read** 가능
- **`cluster-require-full-coverage yes`(기본)**: 16384 슬롯 중 **하나라도 담당 노드가 없으면 클러스터 전체가 `fail`** (`CLUSTERDOWN`).  
  ✅ 검증: 마스터(7002)와 그 레플리카(7005)를 **둘 다** 내리자 `cluster_state:fail`, `slots_ok:11022 / slots_fail:5362`, **살아있는 슬롯의 키 조회도 `CLUSTERDOWN The cluster is down`**.  
  → 일부 서비스 지속이 중요하면 `no` 로 바꿀 수 있다. ✅ 재검증(모든 노드 `no`, 마스터+레플리카 동반 종료로 4096 슬롯 소실, 10초 후): `cluster_state:ok`, `slots_ok:12288 / slots_fail:4096`, **살아있는 슬롯의 키는 정상 읽기/쓰기**. 소실 슬롯 키는 `MOVED`(죽은 노드로 안내)만 돌려줘 **클라이언트가 죽은 노드로 접속 시도 → 타임아웃** 이 나는 형태가 됨. (첫 측정에서 즉시 `ok` 가 안 된 건 반영 시간 문제로 보임)
- 최소 구성: **마스터 3대 이상**(장애 판정·선출이 **마스터 과반수** 투표이므로 마스터 2대는 1대 장애에서 과반 불가) + 마스터당 레플리카 ≥1 권장

### 1-5. 노드 구성 (`nodes.conf`)

- `cluster-config-file`(기본 `nodes.conf`)에 노드ID, 주소, 역할, 슬롯, **configEpoch** 를 **노드가 스스로 갱신·저장** (센티널처럼 수동 편집 금지)
- 재시작하면 이 파일로 복귀 → ✅ 검증: 종료했던 구 마스터(7001)를 다시 띄우자 파일을 읽고 **"Configuration change detected. Reconfiguring myself as a replica of 7004"** 로 **스스로 레플리카로 전환**하고 partial sync 시도

---

## 2부. 클라이언트 라우팅 — MOVED vs ASK

| | `-MOVED slot host:port` | `-ASK slot host:port` |
|---|---|---|
| 의미 | **슬롯의 소유가 영구적으로 바뀜(또는 원래 거기 없음)** | **슬롯 이전 중, 이 키만 일시적으로 저쪽** |
| 클라이언트 동작 | **슬롯→노드 캐시 갱신**(`CLUSTER SLOTS/SHARDS` 재조회) 후 재시도 | **캐시 갱신 안 함**. 해당 요청 1회만 `ASKING` + 명령을 대상 노드로 전송 |
| 발생 시점 | 일반 라우팅 오류, 리샤딩 완료 후, failover 후 | **리샤딩 중(MIGRATING 상태 슬롯)** 에서 키가 이미 옮겨졌거나 없을 때 |

### 2-1. `redis-cli -c` 와 스마트 클라이언트

- `redis-cli` 기본은 리디렉션을 **따라가지 않는다** → `MOVED 12182 ...` 를 그대로 출력 ✅ 검증. `-c` 를 주면 자동으로 따라감 ✅ 검증
- 실제 클라이언트(Jedis/Lettuce/redis-py-cluster 등)는 **시작 시 `CLUSTER SLOTS`(또는 `SHARDS`)로 슬롯 맵을 캐시** 하고 `CRC16` 로 **직접 올바른 노드에 접속** → 평소엔 리디렉션 0회. `MOVED` 를 받으면 **맵 갱신(topology refresh)**
- Lettuce: `ClusterTopologyRefreshOptions` 의 **adaptive refresh**(MOVED/ASK/재연결 시 트리거) 를 켜지 않으면 failover/리샤딩 후 **옛 맵으로 계속 `MOVED`** 를 맞는 문제가 흔함

### 2-2. 리샤딩 중 한 슬롯에서 ASK 가 나오는 정확한 규칙 ✅ 검증

슬롯 11826 을 7003(source, MIGRATING) → 7001(target, IMPORTING)으로 옮기는 중(키 `{u}:a/b/c` 3개 중 `a` 만 먼저 이전):

| 요청 | 결과 | 이유 |
|---|---|---|
| source 에 `GET {u}:b` (아직 source 에 **있음**) | `b` 정상 응답 | 키가 있으면 source 가 처리 |
| source 에 `GET {u}:a` (이미 target 으로 **이전됨**) | `ASK 11826 127.0.0.1:7001` | source 에 없음 → target 에 있을 수 있음 |
| source 에 `GET {u}:zzz` (원래 **없는 키**) | `ASK ...` | **없는 키도 ASK** — 존재 여부를 source 가 판단 못 하고 target 에 물어보게 함 |
| source 에 **새 키 `SET {u}:new`** | `ASK ...` | **신규 쓰기는 target 으로 보내** source 에 키가 늘지 않게 함 (이전이 끝나게 보장) |
| target 에 **ASKING 없이** `GET {u}:a` | `MOVED 11826 127.0.0.1:7003` | IMPORTING 상태여도 `ASKING` 없으면 아직 source 가 소유자 |
| target 에 `ASKING` 후 `GET {u}:a` | 정상 응답 | `ASKING` 은 **다음 한 명령만** 유효 |
| source 에 `MGET {u}:a {u}:b` (**일부는 이전, 일부는 source**) | `TRYAGAIN Multiple keys request during rehashing of slot` | 키들이 두 노드로 갈라져 한 번에 처리 불가 |

→ **슬롯 이전 중에도 서비스는 계속** 되지만, **멀티 키 명령은 `TRYAGAIN`**, 그 외는 ASK 왕복 1회가 추가된다.

---

## 3부. 자동 Failover — 장애 감지부터 리더(마스터) 재선출까지

> 용어 정리: 클러스터에는 센티널의 "리더 센티널"이 없다. 대신 **"장애난 마스터의 레플리카 중 하나가 후보가 되어, 나머지 마스터들의 투표(과반)를 받아 새 마스터(=리더)가 된다."** 이 과정을 **Failover Election** 이라 한다.

### 3-1. 전체 흐름 한눈에

```
 [마스터 M 다운]
     │  (cluster-node-timeout 동안 PONG 없음)
     ▼
 ① PFAIL  : 각 노드가 "내가 보기엔 M 이 죽은 것 같다" (Possible Fail, 주관적)
     │  (gossip 으로 PFAIL 보고가 퍼짐)
     ▼
 ② FAIL   : 슬롯 소유 "마스터" 과반수가 M 을 PFAIL/FAIL 로 보고 → FAIL 확정 + 전 노드에 FAIL 브로드캐스트
     │
     ▼
 ③ 후보 레플리카가 선출 지연(delay) 대기  ← rank 가 높은(offset 큰) 레플리카가 먼저
     │
     ▼
 ④ currentEpoch + 1, 레플리카가 FAILOVER_AUTH_REQUEST 를 모든 마스터에 전송
     │
     ▼
 ⑤ 각 마스터가 조건 확인 후 FAILOVER_AUTH_ACK 투표 (epoch 당 1표)
     │
     ▼
 ⑥ 과반수(마스터 수/2 + 1) 득표 → 승격: 새 configEpoch 로 M 의 슬롯을 인수, PONG 으로 전파
     │
     ▼
 ⑦ 나머지 레플리카는 새 마스터를 복제 대상으로 전환, 구 마스터가 돌아오면 레플리카로 강등
```

### 3-2. ① PFAIL → ② FAIL (장애 감지)

| 단계 | 판정 주체 | 조건 | 의미 |
|---|---|---|---|
| **PFAIL** (Possible FAIL) | **각 노드가 자기 시야로** | 다른 노드에서 **`cluster-node-timeout`** 동안 응답 없음 | 주관적 판단. 단독으로는 failover 못 함 (센티널의 SDOWN 과 유사) |
| **FAIL** | **클러스터 전체 합의** | **슬롯을 가진 마스터 과반수** 가 `node-timeout × 2` 시간 안에 같은 노드를 PFAIL/FAIL 로 보고 | 객관적 확정 → `FAIL` 메시지를 **전 노드에 broadcast** (센티널의 ODOWN 과 유사) |

- **투표권을 가진 건 마스터뿐**. 레플리카의 의견은 FAIL 판정에 **반영되지 않는다**
- ✅ 검증 (node-timeout 3000ms): 07:42:26 마스터 7001 종료 → **07:42:30.96 `Marking node ... as failing (quorum reached)`** (약 4초 후). 다른 마스터 7002 에는 `FAIL message received from ...` 로 전파. 직후 `Cluster state changed: fail`

### 3-3. ③ 선출 지연(Election Delay) — 누가 먼저 도전하나

```
delay = 500ms + random(0 ~ 500ms) + rank × 1000ms        (문서 기준)
```

- **rank** = 같은 마스터의 레플리카들 중 **replication offset 이 더 큰(데이터가 더 최신) 레플리카의 수**. 가장 최신 레플리카 = rank 0
- → **데이터가 가장 최신인 레플리카가 가장 먼저 투표를 요청**한다. 오래된 레플리카는 최소 1초씩 늦게 시작하므로, 최신 레플리카가 이미 이기면 포기
- random 지터는 **동시 도전 충돌(split vote) 방지**
- ✅ 검증: `Start of election delayed for 528 milliseconds (rank #0, offset 5774)` (500 + 랜덤 28ms, rank 0)

### 3-4. ④~⑥ 투표

1. 후보 레플리카: `currentEpoch++` 후 `FAILOVER_AUTH_REQUEST` 를 **모든 마스터** 에게 전송  
   ✅ `Starting a failover election for epoch 8.`
2. 요청을 받은 **마스터**가 **아래 조건을 모두 만족하면** `FAILOVER_AUTH_ACK` (표 1개) — **이 epoch 에서 이미 투표했으면 거절**:
   - 요청자가 레플리카이고, **그 마스터가 FAIL 상태**(수동 failover 의 `FORCEACK` 제외)
   - 요청의 `currentEpoch` ≥ 내 `currentEpoch` (**오래된 epoch 거절**)
   - 같은 epoch 에 **이미 다른 후보에게 투표하지 않음** (`lastVoteEpoch`)
   - 요청자가 주장하는 **슬롯들의 configEpoch 가 내가 아는 값보다 낮지 않음** (옛 정보로 슬롯을 가져가려는 시도 거절)
   - 같은 마스터의 레플리카에게 **`node-timeout × 2` 이내에 이미 투표한 적 없음** (연속 투표 방지)
3. ✅ 검증: 7002 로그 `Failover auth granted to dfb3e237... for epoch 8`
4. 후보가 **`마스터 수/2 + 1`** (3마스터 → 2표, **죽은 마스터 자신 포함 N 기준**) 이상 득표 → **당선**
5. 대기 한도: 요청 후 **`max(node-timeout × 2, 2000ms)`** 안에 과반을 못 얻으면 실패하고, **`그 2배` 시간 뒤 재시도**(새 epoch) (문서 기준)

### 3-5. ⑥~⑦ 승격과 전파 ✅ 검증 (실제 로그)

```
07:42:30.964  Marking node ae2cdb20... as failing (quorum reached).     ← FAIL 확정
07:42:30.965  Cluster state changed: fail
07:42:31.009  Start of election delayed for 528 milliseconds (rank #0, offset 5774).
07:42:31.618  Starting a failover election for epoch 8.
07:42:31.639  Failover election won: I'm the new master.
07:42:31.639  configEpoch set to 8 after successful failover
07:42:31.640  Setting secondary replication ID ...  (PSYNC2: 새 마스터가 옛 replid 를 secondary 로 보관)
07:42:31.640  Cluster state changed: ok
```

- 종료 시점부터 **새 마스터 서비스 재개까지 약 5초** (`node-timeout` 3초 + 선출 ~0.7초 + 감지 시간)
- **그 사이(~0.7초) `cluster_state:fail`** — 이 마스터의 슬롯 범위뿐 아니라(`require-full-coverage yes` 이므로) **클러스터 전체** 가 일시 쓰기 불가
- **구 마스터 복귀** ✅ 검증: 재기동 → `nodes.conf` 읽음 → 더 높은 configEpoch(8)의 7004 를 보고 **스스로 레플리카로 전환**, `Before turning into a replica, using my own master parameters to synthesize a cached master: I may be able to synchronize with the new master with just a partial transfer.` (→ PSYNC2 로 partial sync 시도)
- 새 마스터는 `PONG` 으로 **"슬롯 X 는 이제 내 것(configEpoch 8)"** 을 알리고, 다른 노드는 **더 높은 configEpoch 를 가진 쪽이 슬롯 주인** 이라는 규칙으로 **옛 정보를 덮어쓴다** (분쟁 해결의 기준)

### 3-6. Epoch 두 종류 — 센티널보다 헷갈리는 부분

| Epoch | 범위 | 의미 |
|---|---|---|
| **currentEpoch** | **클러스터 전역(노드마다 보유, gossip으로 최대값 수렴)** | **논리적 시계**. failover 선출마다 +1. "투표 라운드 번호" |
| **configEpoch** | **마스터마다 1개** | 그 마스터의 **슬롯 소유 설정 버전**. 당선 시 새 currentEpoch 값으로 설정. **같은 슬롯을 두 노드가 주장하면 configEpoch 가 큰 쪽이 승리** |

- ✅ 검증: 최초 `epoch 6`(노드마다 다르게 배정) → 리샤딩 `setslot` 으로 7 → failover 로 8 → 수동 failover 로 9. **슬롯 소유권이 바뀔 때마다 epoch 가 올라간다**
- 두 노드가 **동일 configEpoch** 를 갖게 되는 충돌은 **노드ID 사전순으로 더 작은 쪽이 epoch 를 올려** 해소 (문서 기준)

### 3-7. 과반수를 못 얻는 경우 — 선출이 **안 되는** 시나리오

| 상황 | 결과 |
|---|---|
| 마스터 3대 중 **2대가 동시에 죽음** (살아있는 마스터 1대) | 생존 레플리카가 **과반(2표)을 못 얻음** → failover **실패**, 클러스터 계속 `fail` |
| **마스터와 그 레플리카가 같이 죽음** | 승격 후보가 없음 → 슬롯 소실 → `require-full-coverage yes` 면 **전체 `CLUSTERDOWN`** ✅ 검증 |
| 네트워크 파티션으로 **소수 쪽에 마스터 일부+레플리카** | 소수 쪽 레플리카는 **과반 투표 불가** → 승격 못 함. 다수 쪽은 선출 진행 |
| 소수 쪽에 **마스터가 고립** | `node-timeout` 이후 **스스로 쓰기를 거부**(과반 마스터와 통신 불가 시 `cluster_state:fail`) → 구 마스터의 split-brain 쓰기 창이 `node-timeout` 으로 제한됨 |

→ **그래서 클러스터는 "마스터 3대 이상 + 과반이 서로 다른 장애 도메인에"** 가 기본. 센티널과 달리 **쓰기 유실은 비동기 복제 때문에 여전히 가능** (구 마스터가 FAIL 판정 전에 받은 쓰기).

---

## 4부. 복제본 선출 — 자격 조건, 우선순위, 처리 방법

### 4-1. 레플리카가 후보가 되려면 (자격)

레플리카는 `serverCron/clusterHandleSlaveFailover` 에서 아래를 모두 확인한 뒤에야 선출을 시작한다.

| 조건 | 설명 |
|---|---|
| 내 마스터가 **FAIL** (또는 수동 failover 중) | PFAIL 만으로는 시작 불가 |
| 내 마스터가 **슬롯을 1개 이상** 보유 | 슬롯 없는 마스터의 레플리카는 승격 대상 아님 |
| **`cluster-replica-no-failover no`** (기본) | `yes` 면 자동 failover 불참 (수동만 허용). DR 리전 레플리카에 사용 |
| **데이터 신선도(data age)** 합격 | 아래 4-2 |
| 마스터와의 링크가 끊긴 지 **적정 시간** 내 | 너무 오래 끊겼으면 포기 |
| 이번 선출의 **재시도 간격**·**delay** 도달 | 3-3, 3-4 |

### 4-2. 데이터 신선도 — `cluster-replica-validity-factor`

```
data_age = (마스터와 링크가 끊긴 이후 경과 시간)   [링크가 정상일 땐 마지막 통신 이후 시간]
          - node-timeout (이미 FAIL 감지에 소비된 시간은 제외)

max_age = node-timeout × cluster-replica-validity-factor  +  repl-ping-replica-period × 1000

data_age > max_age  →  이 레플리카는 "너무 오래된 데이터" 로 판단, 선출 불참
```

- 기본 `validity-factor = 10` ✅ 확인(`config get`), `node-timeout 3000` → **약 30초 + ping 주기**
- **`0` 으로 설정하면** 데이터가 아무리 오래됐어도 **항상 승격 시도** (가용성 우선, 데이터 유실 위험↑)
- 값이 **작을수록 보수적**: 레플리카가 오래 끊겼던 경우 승격을 포기 → 마스터를 못 되면 **슬롯이 영구 소실**(운영자 개입 필요)

### 4-3. 우선순위 — **레플리카 간 순서**

센티널은 `replica-priority → offset → runid` 를 직접 비교하지만 **클러스터는 "선출 지연(delay)" 으로 순서를 표현** 한다.

| 요소 | 클러스터에서의 영향 |
|---|---|
| **replication offset (rank)** | 3-3 의 `rank × 1000ms` 로 **더 최신 레플리카가 1초씩 일찍 시작** (주 기준) |
| **random 0~500ms** | 같은 rank(=동일 offset)일 때 충돌 방지 |
| `replica-priority` | **클러스터 모드에서는 무시**(센티널/`REPLICAOF` 승격 시에만 사용) — 문서 기준 |
| **투표 가능성** | 마스터들이 위 3-4 의 조건을 만족시켜야만 표를 줌 |

→ "가장 데이터가 최신인 레플리카가 이길 확률이 가장 높다"를 **시간차로 구현** 한 것이지, "오래된 레플리카는 절대 못 이긴다"는 보장은 아니다. (최신 레플리카가 늦게 뜨면 다른 레플리카가 당선될 수 있고, 그만큼 **데이터 유실**)

### 4-4. 선출 이후 처리

1. 당선 레플리카: **자신을 마스터로 전환** (`clusterSetNodeAsMaster`), 기존 마스터의 **슬롯을 모두 인수**(`clusterDelSlot`→`clusterAddSlot`), **`configEpoch` = 당선 epoch**
2. **새 replid 생성**, 이전 replid 는 **secondary replid(`shiftReplicationId`)** 로 보관 → **다른 레플리카가 partial resync(PSYNC) 로 따라올 수 있음**
3. 즉시 **PONG 브로드캐스트** → 모든 노드가 슬롯 맵·epoch 갱신
4. 같은 마스터의 **다른 레플리카** 들은 새 마스터를 복제 대상으로 변경 (✅ 검증: `cluster nodes` 로 7001=slave of 7004 확인)
5. **구 마스터가 복귀하면** 설정 충돌을 감지하고 레플리카로 강등 ✅ 검증 (3-5 참고)

### 4-5. 레플리카 마이그레이션 (Replica Migration)

- 어떤 마스터의 레플리카가 모두 사라지면(고아 마스터), **레플리카가 여분으로 많은(≥ `cluster-migration-barrier + 1`) 다른 마스터의 레플리카가 자동 이동** 해서 보강 (`cluster-allow-replica-migration yes` 기본, `cluster-migration-barrier 1` ✅ 확인)
- 효과: **마스터당 레플리카 1개** 정도의 최소 비용으로 연쇄 장애에도 견디는 구조 (마스터 A 의 레플리카가 죽으면 레플리카 2개를 가진 B 의 레플리카가 A 로 이동)
- 이 이동으로 **승격 후보가 항상 존재** 하게 되어 슬롯 소실 가능성을 낮춤
- ✅ 검증: 마스터 7003 에 레플리카 2개(7005, 7009), 마스터 7007 에 레플리카 1개(7008) 상태에서 **7008 을 종료**하자 약 15초 안에 로그 `Migrating to orphaned master 6cddb47b...(7007)` 와 함께 **7005 가 7007 의 레플리카로 이동**. 7003 은 7009 하나가 남아 `migration-barrier 1` 을 만족

---

## 5부. 수동 Failover — CLUSTER FAILOVER 3종

레플리카에서 실행 (`CLUSTER FAILOVER [FORCE | TAKEOVER]`).

| 모드 | 동작 | 합의 | 데이터 유실 | 용도 |
|---|---|---|---|---|
| **(기본)** | ① 마스터에게 **클라이언트 쓰기 중단**(pause) 요청 ② **마스터의 복제 스트림을 끝까지 따라잡음**(offset 일치) ③ 선출 진행 | **필요**(마스터 과반 투표) | **없음** (offset 맞춘 뒤 전환) | **무중단 롤링 업그레이드/유지보수**의 표준 방법 |
| **FORCE** | 마스터와 **핸드셰이크·동기화 생략**, 바로 선출 | **필요**(과반 투표) | **있을 수 있음** | **마스터가 죽었지만 FAIL 판정 전**일 때 강제로 승격 |
| **TAKEOVER** | **투표 없이** 자신의 `configEpoch` 를 **강제로 올리고** 슬롯 인수 | **불필요** | 있을 수 있음 | **마스터 과반이 죽은 상황**에서 복구 (다른 노드의 의견 무시 → 신중히) |

✅ 검증 (기본 모드): 7001 이 7004 로부터 마스터를 되찾음
```
All master replication stream processed, manual failover can start.
Start of election delayed for 0 milliseconds (rank #0, offset 5774).     ← 수동은 delay 0
Starting a failover election for epoch 9.
Failover election won: I'm the new master.
```
- 자동 failover 와 달리 **FAIL 판정·지연 없이** 바로 시작, 전환 시간이 **약 20ms** 수준
- 수동 failover 도 **`cluster-node-timeout × 2`(최대 5초, 문서 기준) 안에 완료 못 하면 취소**

✅ 검증 (FORCE): 방금 레플리카로 강등된 7001 에서 `CLUSTER FAILOVER FORCE`
```
Start of election delayed for 0 milliseconds (rank #0, offset 196).
Starting a failover election for epoch 9.
Failover election won: I'm the new master.        ← 선출 투표는 거침 (epoch 9), 마스터와의 offset 동기화만 생략
```
✅ 검증 (TAKEOVER): 7006 에서 `CLUSTER FAILOVER TAKEOVER`
```
New configEpoch set to 10                          ← 선출 로그("Starting a failover election") 없음, 투표 없이 epoch 만 올림
```
- 결과 `cluster nodes` 에서 7006=master(epoch 10), 7001=slave(epoch 10) 로 정상 수렴. **투표 없이 일방적으로 epoch 를 올리므로** 파티션 상황에서 양쪽이 각자 TAKEOVER 하면 **epoch 충돌 → 사후 병합 시 한쪽 쓰기 유실** 위험(문서 기준)

---

## 6부. 리샤딩(Resharding) 절차와 발생 가능한 이슈

### 6-1. 수동 절차 (슬롯 1개 기준) ✅ 검증

`redis-cli --cluster reshard` 가 아래를 슬롯마다 자동 수행한다.

```
# ① target 을 IMPORTING, source 를 MIGRATING 으로 표시
CLUSTER SETSLOT <slot> IMPORTING <source-id>       (target 에서)
CLUSTER SETSLOT <slot> MIGRATING <target-id>       (source 에서)

# ② 슬롯의 키를 배치로 옮김 (원자적, 이전 중 source 가 잠깐 블로킹)
CLUSTER GETKEYSINSLOT <slot> <count>
MIGRATE <host> <port> "" 0 <timeout> KEYS k1 k2 ...        (키가 남아 있지 않을 때까지 반복)

# ③ 소유권 확정 (target 먼저 → 그다음 source 와 다른 노드에도)
CLUSTER SETSLOT <slot> NODE <target-id>
```

- `cluster nodes` 에서 진행 상태를 볼 수 있음 ✅ 검증: `[11826->-<target-id>]`(source, MIGRATING) / `[11826-<-<source-id>]`(target, IMPORTING)
- ③ 시점에 **configEpoch 가 올라가고** 전 노드에 전파됨 ✅ 검증 (epoch 6→7)
- ✅ 검증: `redis-cli --cluster reshard ... --cluster-from <id> --cluster-to <id> --cluster-slots 100` → **7002→7001 로 100 슬롯 이동, 키 504개 유지, `All 16384 slots covered`**
- 노드를 **추가**: `--cluster add-node new:port existing:port` → **슬롯 0개** 이므로 `reshard`/`rebalance` 로 슬롯을 받아야 의미가 있음. 레플리카로 붙이려면 `--cluster-slave [--cluster-master-id <id>]`

### 6-2. 이슈 정리

| # | 이슈 | 원인 / 증상 | 대응 |
|---|---|---|---|
| 1 | **ASK 리디렉션 증가** | 슬롯 이전 중 **없는/이전된 키가 모두 ASK** (2-2). 요청 1건당 **왕복 2회** | 클라이언트가 **ASK 를 정상 처리**해야 함(`ASKING` + 재전송, 맵 갱신 금지). 직접 구현 시 **ASK 를 MOVED 처럼 캐시하면 안 됨** |
| 2 | **`TRYAGAIN`** | 멀티 키/파이프라인에서 같은 슬롯의 키가 **source·target 에 갈라짐** | **짧게 재시도**(백오프). `MGET` 은 키별로 쪼개거나 해시태그 사용 시 리샤딩 중 실패가 늘 수 있음을 인지 |
| 3 | **`MIGRATE` 블로킹(대형 키)** | `MIGRATE` 는 **키 단위로 직렬화→전송→삭제** 를 **메인 스레드에서 동기 수행** → **big key(수백 MB 해시/셋/리스트)** 는 **수 초~수십 초 블로킹**, 클라이언트 타임아웃/failover 오판(`node-timeout` 초과 시 PFAIL) | **big key 를 사전에 쪼개기**, 리샤딩 전 `MEMORY USAGE`/`--bigkeys` 점검, `--cluster-pipeline`(배치 키 수)·`MIGRATE` timeout 조정, **트래픽 적은 시간대** 진행 |
| 4 | **리샤딩 중 failover** | source/target 이 죽으면 **MIGRATING/IMPORTING 상태가 `nodes.conf` 에 남음**(open slot) → `redis-cli --cluster check` 가 "open slots" 경고 | `redis-cli --cluster fix <host:port>` 로 복구(`SETSLOT STABLE` 또는 마저 이전), 후 재개 |
| 5 | **클라이언트의 슬롯 캐시 낡음** | 리샤딩 후 `MOVED` 폭주 | **topology refresh**(주기/적응형) 설정, MOVED 시 맵 갱신 |
| 6 | **target 용량 부족** | 슬롯을 받는 노드가 `maxmemory` 도달 → eviction/OOM 에러 | 사전 용량 점검, `maxmemory-policy` 확인 |
| 7 | **복제 지연·메모리 증가** | `MIGRATE` 가 키마다 `DEL`(source) + `SET`(target) → source/target 의 **replica 에 복제 트래픽 증가**, 둘 다 **AOF rewrite/RDB fork 와 겹치면 지연** | 백업/rewrite 시간대 피하기, **한 번에 옮기는 슬롯 수 제한** |
| 8 | **Lua/트랜잭션 실패** | 이전 중인 슬롯의 키를 대상으로 한 `MULTI/EXEC`, `EVAL` 은 키가 갈라지면 실패(`TRYAGAIN`) | 리샤딩 시간대 **스크립트/트랜잭션 최소화** |
| 9 | **레플리카 읽기** | `READONLY` 레플리카는 리샤딩 후 **자신의 마스터가 더 이상 그 슬롯의 소유자가 아니면 `MOVED`** | 클라이언트가 읽기 분산 시 맵 갱신 필요 |
| 10 | **키스페이스 이벤트/Pub/Sub** | 일반 Pub/Sub 은 **슬롯과 무관한 broadcast**(6장) → 리샤딩 영향 없음. **Sharded Pub/Sub 채널은 슬롯 이전 시 구독이 끊김**(`sunsubscribe` 푸시) | Sharded Pub/Sub 클라이언트는 **재구독 로직** 필요 |

### 6-3. 리샤딩 전 점검 체크리스트

- [ ] `redis-cli --cluster check` 로 **open slots / 불일치 없음**
- [ ] **big key** 점검 (`--bigkeys` / `MEMORY USAGE`), 가장 큰 키 크기와 `cluster-node-timeout` 비교
- [ ] 모든 노드 **같은 버전**(혼합 시 `MIGRATE` 포맷 호환성 확인)
- [ ] 클라이언트의 **ASK/MOVED/TRYAGAIN 처리, topology refresh** 확인
- [ ] 복제 지연(`INFO replication`)과 **백업/rewrite 시간대 충돌** 확인

---

## 7부. CLUSTER FORGET / 노드 제거 시 이슈

### 7-1. CLUSTER FORGET 이란

`CLUSTER FORGET <node-id>` : **그 노드를 내 노드 테이블에서 제거** 하고 **60초 동안 블랙리스트**에 올려 gossip 으로 다시 학습하지 않게 한다.

- 불가: ✅ 검증 `ERR I tried hard but I can't forget myself...`, `ERR Can't forget my master!`

### 7-2. 이슈와 대응

| # | 이슈 | 내용 | 대응 |
|---|---|---|---|
| 1 | **일부 노드에서만 forget → 재등록 (구버전 동작)** | 클래식 동작은 **forget 이 로컬에만 적용**. 다른 노드 gossip 으로 **60초 블랙리스트 만료 후 다시 추가**되는 문제. 그래서 **"모든 노드에서 60초 안에 forget"** 이 정석이었음 | `redis-cli --cluster del-node` 는 **모든 노드에 FORGET 을 보내고** 대상 노드를 `RESET` 해줌 |
| 2 | **✅ Redis 7.2.6 실측: forget 이 다른 노드로 전파됨** | 7001 에서만 `CLUSTER FORGET <7007>` 실행 → **2초 뒤 6개 노드 모두에서 7007 이 사라졌고**, 67초 후에도 재등록 안 됨 (재등록 시도 후 실패). 슬롯 소유 마스터(7003)를 forget 해도 **7002 에서도 `known_nodes 5`** 로 같이 제거됨 | **Redis 7.x 에서 forgotten-node 정보를 PING 확장 헤더에 실어 전파**(문서 기준)하는 것으로 보이나, **구체 버전/TTL 은 로컬에서 확인하지 못함**. 혼합 버전 클러스터에선 **구버전 동작 가정**하고 모든 노드에 forget |
| 3 | **슬롯 소유 마스터를 forget** | 슬롯이 **미할당(uncovered)** → ✅ 검증: `cluster_slots_assigned:10924`(5460 소실), **`cluster_state:fail`**, `CLUSTERDOWN Hash slot not served` | **반드시 `reshard` 로 슬롯을 비운 뒤** forget. 슬롯을 소유한 마스터는 `del-node` 가 거부 |
| 4 | **레플리카 달린 마스터 제거** | 마스터 삭제 전 **슬롯을 다른 마스터로 이전** 했더라도 레플리카는 **다른 마스터를 복제하도록 이동**하거나 함께 제거해야 함 (없으면 레플리카 고립/replica migration 으로 자동 이동) | 레플리카 먼저 정리 → 마스터 슬롯 이전 → `del-node` |
| 5 | **FAIL 상태의 죽은 노드 정리** | 영구 장애 노드가 `cluster nodes` 에 `fail` 로 남음 → 영원히 PFAIL/FAIL 판정 로그 | 모든 노드에서 `CLUSTER FORGET`(또는 `del-node`) |
| 6 | **forget 후 대상 노드가 계속 살아있음** | 대상이 다른 노드를 여전히 알고 있어 **다시 MEET** 하거나 클러스터에 재가입(`nodes.conf` 보유) | 대상에서 **`CLUSTER RESET [HARD]`** 후 종료. 슬롯/데이터가 남은 노드는 `FLUSHALL` 필요 |
| 7 | **forget 대상 마스터의 레플리카가 같은 노드 이름으로 재등장** | 노드 ID 는 `nodes.conf` 에 고정 → 같은 `nodes.conf` 로 재기동하면 **옛 정체성으로 복귀**해 충돌 | 제거한 노드는 **`nodes.conf` 삭제** 후 재사용 |
| 8 | **`require-full-coverage` + forget** | 위 3번에서 클러스터 **전체**가 fail | 필요 시 임시 `cluster-require-full-coverage no`(권장하지 않음) |

### 7-3. 안전한 노드 제거 순서

```
1) redis-cli --cluster reshard ...   # 대상 마스터의 슬롯을 전부 다른 마스터로 이동
2) redis-cli --cluster check          # 슬롯 0개, open slot 없음 확인
3) (레플리카가 있으면) 다른 마스터로 이동하거나 먼저 del-node
4) redis-cli --cluster del-node <host:port> <node-id>   # 모든 노드에 FORGET + 대상 RESET
5) 대상 서버 종료, nodes.conf 삭제
```

---

## 8부. 주요 설정과 명령어 요약

### 8-1. 설정 (✅ 기본값은 7.2.6 `config get` 으로 확인)

| 설정 | 기본값 | 의미 |
|---|---|---|
| `cluster-enabled` | `no` | 클러스터 모드 활성화 |
| `cluster-config-file` | `nodes.conf` | 노드가 스스로 관리하는 설정 파일 |
| `cluster-node-timeout` | `15000` (로컬 테스트에선 3000) | **노드를 PFAIL 로 볼 때까지의 무응답 시간**. failover 속도·민감도 결정 (문서 기준 기본값) |
| `cluster-replica-validity-factor` | `10` | 4-2, 오래된 레플리카 승격 제외 기준 배수 |
| `cluster-require-full-coverage` | `yes` | 슬롯 하나라도 비면 전체 fail |
| `cluster-allow-reads-when-down` | `no` | fail 상태에서도 읽기 허용(일관성보다 가용성) |
| `cluster-migration-barrier` | `1` | 레플리카 마이그레이션 시 마스터가 남겨야 하는 최소 레플리카 수 |
| `cluster-allow-replica-migration` | `yes` | 레플리카 자동 이동 허용 |
| `cluster-replica-no-failover` | `no` | `yes` 면 자동 failover 불참 |
| `cluster-link-sendbuf-limit` | `0` | 클러스터 버스 링크 송신 버퍼 상한(메모리 보호) |
| `cluster-announce-ip/port/bus-port` | — | NAT/Docker/k8s 에서 외부에 알릴 주소 |

### 8-2. 명령어

| 명령 | 용도 |
|---|---|
| `CLUSTER INFO` | 상태(`cluster_state`, slots_ok/fail, known_nodes, epoch) |
| `CLUSTER NODES` / `CLUSTER SHARDS` / `CLUSTER SLOTS` | 노드·슬롯·역할 조회 (`SHARDS` 7.0+, 권장) |
| `CLUSTER KEYSLOT key` | 키의 슬롯 번호 |
| `CLUSTER COUNTKEYSINSLOT slot` / `GETKEYSINSLOT slot n` | 슬롯 내 키 수/목록 |
| `CLUSTER MEET ip port` | 노드를 클러스터에 합류시킴 (gossip 시작) |
| `CLUSTER ADDSLOTS` / `DELSLOTS` / `ADDSLOTSRANGE` | 슬롯 수동 할당/제거 |
| `CLUSTER SETSLOT slot IMPORTING|MIGRATING|NODE|STABLE` | 슬롯 이전 상태 제어 |
| `CLUSTER REPLICATE node-id` | 이 노드를 해당 마스터의 레플리카로 |
| `CLUSTER FAILOVER [FORCE|TAKEOVER]` | 수동 failover (레플리카에서) |
| `CLUSTER FORGET node-id` | 노드 제거(60초 블랙리스트) |
| `CLUSTER RESET [HARD|SOFT]` | 노드 정보 초기화 |
| `CLUSTER SAVECONFIG` | `nodes.conf` 즉시 저장 |
| `READONLY` / `READWRITE` | 레플리카 읽기 허용/해제 |
| `ASKING` | ASK 리디렉션 대상 노드에서 **다음 1개 명령** 허용 |
| `redis-cli --cluster create/check/fix/reshard/rebalance/add-node/del-node/call` | 운영 도구 |

---

## 9부. 센티널 vs 클러스터, 한계

| 구분 | 센티널(9장) | 클러스터 |
|---|---|---|
| 목적 | **HA**(자동 failover) | **HA + 수평 확장(샤딩)** |
| 쓰기 확장 | 불가(마스터 1) | **가능**(마스터 N) |
| 최소 구성 | 센티널 3 + 마스터1 + 레플리카1 | **마스터 3 + 레플리카 3 (권장)** |
| 감시 주체 | 별도 센티널 프로세스 | **데이터 노드 자체**(gossip) |
| 장애 판정 | SDOWN/ODOWN (quorum) | **PFAIL/FAIL (마스터 과반)** |
| 승격 결정 | 센티널 리더가 후보 선정 | **레플리카가 스스로 후보가 되어 마스터 과반 투표** |
| 클라이언트 | 센티널에 주소 질의 | **스마트 클라이언트 + MOVED/ASK** |
| 멀티 키/트랜잭션 | 제약 없음 | **같은 슬롯만**(해시태그) |
| DB 수 | 16개(`SELECT`) | **DB 0 만** |

### 클러스터의 한계

- **멀티 키 연산 제약**(CROSSSLOT), **`SELECT` 불가**, `KEYS`/`SCAN` 은 **노드마다 따로** 실행
- **비동기 복제**로 인한 **쓰기 유실 가능**(WAIT 로 완화, 근본 해결 아님)
- **마스터 과반 장애**나 **슬롯 소유 마스터+레플리카 동반 장애**에는 **자동 복구 불가**
- 노드 수 상한 **약 1000** (gossip 오버헤드, 16384 슬롯 비트맵)
- **클라이언트 호환성**: 클러스터 지원 클라이언트 필수

---

## 10부. 운영 심화 — 구축·네트워크·클라이언트·튜닝

### 10-1. 클러스터 구축과 확장 (create / add-node / rebalance)

```bash
# 최초 구성 (마스터 3 + 레플리카 3, --cluster-replicas 1)
redis-cli --cluster create h1:6379 h2:6379 h3:6379 h4:6379 h5:6379 h6:6379 --cluster-replicas 1

# 노드 추가 — 기본은 "슬롯 0개 마스터"
redis-cli --cluster add-node new:6379 existing:6379

# 레플리카로 추가 (마스터 지정 안 하면 레플리카가 가장 적은 마스터에 붙음)
redis-cli --cluster add-node new:6379 existing:6379 --cluster-slave [--cluster-master-id <id>]

# 슬롯 재분배
redis-cli --cluster rebalance existing:6379 [--cluster-weight id=2 ...] [--cluster-use-empty-masters]
```

✅ 검증 (3 마스터에 빈 마스터 7007 추가 후):
| 명령 | 결과 |
|---|---|
| `rebalance` (옵션 없음) | **`*** No rebalancing needed! All nodes are within the 2.00% threshold.`** — 빈 마스터는 **기본적으로 대상에서 제외** (슬롯 0개라 그대로 방치되기 쉬운 함정) |
| `rebalance --cluster-use-empty-masters` | 4마스터가 **각 4096 슬롯** 으로 균등 재분배 (`7007: [0-1364],[5461-6826],[10923-12287]`) |
| `add-node --cluster-slave --cluster-master-id <7007>` | 7008 이 7007 의 레플리카로 정상 합류 |

- `--cluster-weight` 로 **성능이 다른 노드(큰 인스턴스)에 슬롯을 더 많이** 배정 가능
- `--cluster-threshold`(기본 2%) 이내 편차면 rebalance 안 함
- **`create` 는 반드시 빈 노드**(데이터/`nodes.conf` 없는)에서 실행. 기존 데이터가 있는 노드는 `CLUSTER RESET` + `FLUSHALL` 필요
- 슬롯을 **수동 지정**하고 싶으면 `CLUSTER ADDSLOTSRANGE` 후 `MEET` 로 구성할 수도 있음(자동화/Operator 에서 사용)

### 10-2. Docker / Kubernetes / NAT 환경 — announce 설정

클러스터는 **노드가 서로에게 자기 주소를 광고**하는 구조 → **컨테이너 내부 IP ≠ 외부에서 접근하는 IP** 이면 `MOVED` 가 **클라이언트가 못 가는 주소** 로 나가거나 gossip 이 깨진다.

| 설정 | 의미 |
|---|---|
| `cluster-announce-ip` | 다른 노드/클라이언트에 광고할 IP (NAT 외부 IP, Pod IP) |
| `cluster-announce-port` / `cluster-announce-bus-port` | 광고할 데이터/버스 포트 (포트 매핑이 다를 때) |
| `cluster-announce-hostname` | 광고할 호스트명 (IP 가 바뀌는 k8s StatefulSet 에서 유용, 7.0+) |
| `cluster-preferred-endpoint-type` | `ip`(기본) / `hostname` / `unknown-endpoint` — **`MOVED`/`CLUSTER SLOTS` 에 어떤 주소를 쓸지** |
| `cluster-announce-human-nodename` | 로그용 사람이 읽는 이름 (7.2+) |

✅ 검증 (7.2.6): 7001 만 `cluster-announce-hostname node1.example.local` + `cluster-preferred-endpoint-type hostname` 로 바꾼 뒤
```
7006> SET foo bar      → MOVED 12182 127.0.0.1:7007       (7006 은 ip 모드)
7001> SET foo bar      → MOVED 12182 ?:7007                ← hostname 모드인데 대상(7007)이 hostname 을 광고하지 않음 → "?" 로 출력
```
- **함정**: `preferred-endpoint-type hostname` 은 **모든 노드가 hostname 을 광고해야** 의미가 있다. 일부 노드만 설정하면 클라이언트가 **`?` 주소를 받아 접속 실패**
- 버스 포트는 **데이터 포트 + 10000**(기본). Docker 에서 포트 매핑 시 **버스 포트도 매핑/오픈** 해야 하고, `host` 네트워크 모드가 가장 단순
- k8s: `StatefulSet` + `headless Service`, `nodes.conf` 를 **PVC 에 영속화** 해야 Pod 재시작 후 **같은 노드ID/슬롯** 으로 복귀 (IP 가 바뀌어도 `announce-ip`/hostname + gossip 으로 갱신)

### 10-3. 클라이언트 설정 (Lettuce / Jedis / Spring Data Redis)

| 항목 | 설명 | 권장 |
|---|---|---|
| **초기 노드 목록(seed)** | 접속 후 `CLUSTER SLOTS/SHARDS` 로 전체 토폴로지를 학습 → **일부만 적어도 됨**, 단 **2~3개 이상**(한 노드 다운 대비) | 서로 다른 AZ 노드 여러 개 |
| **Topology Refresh** | 장애/리샤딩 후 슬롯 맵 갱신 | **Lettuce**: `ClusterTopologyRefreshOptions` 의 `enablePeriodicRefresh(30s)` + **`enableAllAdaptiveRefreshTriggers()`**(MOVED/ASK/PERSISTENT_RECONNECTS/UNKNOWN_NODE) — **기본값은 둘 다 꺼짐**. 안 켜면 failover 후 옛 맵으로 계속 `MOVED` |
| **`maxRedirects`** | 한 요청의 MOVED/ASK 재시도 횟수(Spring `spring.data.redis.cluster.max-redirects`, 기본 5) | 리샤딩/failover 동안 **너무 작으면 `ClusterRedirectException`** |
| **`validateClusterNodeMembership`** | 토폴로지에 없는 노드 접속 거부(Lettuce 기본 true) | NAT/hostname 환경에서 **주소 불일치로 접속 거부** 되면 false 로 완화 |
| **읽기 분산** | Lettuce `ReadFrom.REPLICA_PREFERRED` 등. 내부적으로 `READONLY` | **stale read 허용** 용도에만. 쓰기 직후 읽기는 마스터로 |
| **재시도/타임아웃** | failover 에는 **약 `node-timeout` + 선출 시간(약 5초)** 소요 | 클라이언트 command timeout 과 재시도 정책을 **5~10초 failover 창을 견디도록** 설계 |
| **파이프라인/멀티 키** | 라이브러리가 **슬롯별로 쪼개서** 전송(Lettuce/Jedis 자동) | 해시태그로 같은 슬롯에 묶지 않으면 **원자성 없음** |

### 10-4. 파티션 상황의 쓰기 유실 창과 완화

```
 [소수 파티션]                        [다수 파티션]
 구 마스터 M (아직 클라이언트 연결됨)   레플리카 R → node-timeout 후 FAIL 판정 → 선출 → 새 마스터
        │  ← 이 시간(≤ node-timeout) 동안 받은 쓰기는                 │
        └────── 파티션 복구 시 버려짐 (R 로 복제 안 됨) ───────────────┘
```

- 소수 파티션의 구 마스터는 **`node-timeout` 이상 과반 마스터와 통신이 안 되면 스스로 쓰기를 거부**(`cluster_state:fail`) → **유실 창이 대략 `node-timeout`** 으로 제한 (문서 기준, 실험은 단일 호스트라 파티션 재현 못함)
- 복구 후 구 마스터는 **더 높은 configEpoch 를 보고 레플리카로 강등**, 자신의 미복제 쓰기는 **폐기** ✅ (강등은 SIGSTOP 실험으로 검증, 아래 10-5)
- 완화:
  - `WAIT <numreplicas> <timeout>` — 레플리카 ACK 까지 대기. ✅ 검증: `SET` 후 `WAIT 1 1000` → `1`. **단 WAIT 는 강한 일관성이 아님**(ACK 전 마스터 장애 시 유실 가능, 승격 레플리카가 ACK 한 쪽이 아닐 수 있음)
  - `min-replicas-to-write` / `min-replicas-max-lag` — 레플리카가 부족하면 마스터가 쓰기 거부(클러스터에서도 동일 적용, 8장)
  - 파티션 가능성이 높은 환경이면 **마스터 과반이 같은 장애 도메인에 있지 않게** 배치

### 10-5. `cluster-node-timeout` 튜닝과 오탐 failover(flapping)

`node-timeout` 은 **PFAIL 판정 기준** 이자 **FAIL/선출/투표 잠금/수동 failover 타임아웃 등 거의 모든 타이머의 기준** 이다.

| | 작게 (예: 3s) | 크게 (예: 15s 기본, 30s) |
|---|---|---|
| failover 속도 | 빠름(약 5초) | 느림(감지만 15초+) |
| 오탐 | **GC/`fork`(RDB·AOF rewrite)/CPU 포화/네트워크 지터** 로 응답 지연 시 **불필요한 failover(flapping)** | 오탐 적음 |
| 수동 failover/리샤딩 | big key `MIGRATE` 가 `node-timeout` 을 넘으면 PFAIL 위험 | 여유 |

✅ 검증 (`node-timeout 3000`, `kill -STOP` 으로 마스터 프로세스 정지):
| 정지 시간 | 결과 |
|---|---|
| **2초** (< 3초) | 다른 노드가 PFAIL 도 안 올림, **failover 없음**, 서비스 지속 |
| **8초** (> 3초) | `master,fail` 로 판정 → 레플리카(7006) 승격. `kill -CONT` 로 되살리자 **로그 `Configuration change detected. Reconfiguring myself as a replica of ...`** 후 **스스로 레플리카로 강등** |

- 실전: **`fork` 시간이 `node-timeout` 에 근접**(대용량 인스턴스, THP 켜짐 등)하면 RDB/AOF rewrite 순간마다 오탐 가능 → **`node-timeout` 늘리기, 인스턴스 크기 제한(마스터당 수~수십 GB 이하), THP off**
- 같은 이유로 **`DEBUG SLEEP`, 긴 Lua, `KEYS *`** 도 클러스터에서는 "죽은 노드"로 오인될 수 있음

### 10-6. Gossip 부하와 수렴 속도 (왜 노드 상한이 ~1000 인가)

- 노드는 **초당 약 10회** 크론 주기마다 **무작위 몇 노드에 PING**(+ 마지막 PONG 이 `node-timeout/2` 를 넘긴 노드에는 **반드시** PING). 각 PING/PONG 에는 **16384 bit 슬롯 비트맵(2KB) + 다른 노드 몇 개의 gossip 섹션**(보통 노드 수의 약 1/10, 최소 3개) (문서 기준)
- 결과: 클러스터 전체의 **메시지량 ≈ O(N²)** 으로 증가, 장애 정보 전파도 `node-timeout` 대비 느려짐 → **노드가 수백 개를 넘으면 `node-timeout` 도 늘리는 것이 안전**
- ✅ 참고 관찰: `MEET` 직후 6노드 클러스터에서 새 노드가 모든 노드에 알려지기까지 **약 2~3초**(초기 gossip 전파)
- 유용한 진단: `CLUSTER LINKS`(버스 링크 상태·송신 버퍼), `CLUSTER COUNT-FAILURE-REPORTS <id>`(그 노드에 대한 FAIL 보고 수)

### 10-7. 클러스터 버스(링크)와 복제 링크는 별개

| 링크 | 포트 | 용도 | 끊기면 |
|---|---|---|---|
| **데이터/복제** | 6379 | 클라이언트 명령 + 마스터→레플리카 **복제 스트림** | 복제 지연/끊김. 클러스터 합의와는 무관 |
| **클러스터 버스** | 16379 (+10000) | **gossip/failure 보고/선출 투표/슬롯 갱신** | **그 노드가 PFAIL/FAIL 로 판정** 될 수 있음 |

- 즉 **데이터 포트는 살아있고 버스 포트만 막히면** → 데이터는 정상인데 클러스터는 "죽었다"고 판단해 **불필요한 failover**. **반대(버스 정상, 복제 링크 끊김)** → 클러스터는 정상으로 보이나 **레플리카 데이터가 낡아가며 승격 자격(4-2 data age) 을 잃을 수 있음**
- 방화벽/보안그룹에서 **양쪽 포트 모두 노드 간 허용**(AWS SG 에서 흔한 실수)

### 10-8. Sharded Pub/Sub 와 클러스터

6장의 `SSUBSCRIBE/SPUBLISH` 는 **채널 이름도 키처럼 슬롯에 매핑**된다.

✅ 검증: `CLUSTER KEYSLOT ch1` → 9930(7002 소유). 7001·7003 에 `SPUBLISH ch1 hi` → **`MOVED 9930 127.0.0.1:7002`**. 즉 **채널의 슬롯 소유 노드(와 그 레플리카)만 처리** → 일반 `PUBLISH` 처럼 전 노드로 broadcast 되지 않음
- 구독자는 **그 채널 슬롯의 노드(마스터 또는 레플리카)** 에 연결, 리샤딩/failover 로 슬롯이 이동하면 **서버가 `sunsubscribe` 푸시로 구독을 끊음** → **클라이언트가 재구독** 해야 함 (문서 기준, 재현 안 함)
- 여러 채널을 한 번에 구독할 땐 **같은 슬롯**(해시태그 `{room1}:chat`, `{room1}:presence`)이어야 함

### 10-9. Redis 7.x 클러스터 개선 요약

| 항목 | 내용 |
|---|---|
| `CLUSTER SHARDS` (7.0) | `CLUSTER SLOTS`(deprecated) 대체. 샤드 단위(마스터+레플리카)·health·replication-offset 제공 ✅ 확인 |
| hostname 지원 (7.0) | `cluster-announce-hostname`, `cluster-preferred-endpoint-type` (10-2) |
| Sharded Pub/Sub (7.0) | 10-8 |
| 멀티 파트 AOF (7.0) | AOF 가 base+incr+manifest 로 나뉨(7장) — 클러스터 노드 각각이 독립 적용 |
| **forgotten node 전파** | ✅ 7.2.6 에서 **`CLUSTER FORGET` 한 번으로 6개 노드 전체에서 제거** 관찰(7-2) — **예전에는 "모든 노드에 60초 내 실행"** 이 정석이었음. 정확한 메커니즘은 PING 확장 헤더로 알려져 있으나 로컬 미확인 |
| `CLUSTER LINKS` (7.0) / `cluster-link-sendbuf-limit` | 느린 노드에 대한 송신 버퍼가 무한히 커지는 문제 방지(0=무제한) |
| 로그 가독성 (7.2) | `cluster-announce-human-nodename` |

---

### ⚠️ 확인하지 못한 부분 (정직 기록)

**로컬에서 직접 재현해 확인한 것**: 해시슬롯/해시태그/`CROSSSLOT`/`MOVED`/`ASK`/`TRYAGAIN`, 수동 리샤딩 절차, 자동 failover 로그(FAIL 확정·선출 지연 528ms·epoch 8·투표), 구 마스터 복귀 시 강등, 기본/FORCE/TAKEOVER 수동 failover, 레플리카 마이그레이션, `require-full-coverage yes/no`, `node-timeout` 오탐(SIGSTOP 2초/8초), add-node/rebalance, hostname announce, Sharded Pub/Sub `MOVED`, `WAIT`, `CLUSTER FORGET`(전파 관찰).

**문서/소스 지식이며 재현하지 않은 것**:
- 선출 delay 공식 `500 + random(0~500) + rank×1000ms`, auth timeout `max(2×node-timeout, 2s)`, retry `×2`, `data_age` 세부 계산식, 투표 거절 조건 5가지, 수동 failover 5초 타임아웃
- `replica-priority` 가 클러스터에서 무시된다는 점
- **소수 파티션 마스터의 자체 쓰기 중단**, TAKEOVER 동시 실행 시 epoch 충돌과 쓰기 유실 (단일 호스트라 네트워크 파티션 재현 불가)
- `cluster-allow-reads-when-down`
- gossip 수치(초당 PING 횟수, 1/10 gossip), `CLUSTER LINKS` 출력 상세
- **리샤딩 중 failover**, **big key `MIGRATE` 블로킹**, **Sharded Pub/Sub 구독 끊김(`sunsubscribe`)**
- **forget 전파의 내부 메커니즘**(확장 헤더·TTL)과 구버전의 60초 블랙리스트 재학습 문제: 관찰만 했을 뿐 원인 미확인
- 클라이언트 라이브러리(Lettuce/Jedis/Spring) 설정 기본값은 각 라이브러리 문서 기준이며 이 환경에서 실행해보지 않음

**한계/주의**: `cluster-require-full-coverage no` 재검증에서 소실 슬롯의 키는 `CLUSTERDOWN` 이 아니라 **죽은 노드로의 `MOVED`** 가 나왔다. 이는 단일 호스트 테스트 결과로, 클라이언트 동작은 라이브러리에 따라 다를 수 있다.
