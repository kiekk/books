### CHAPTER 6 레디스를 메시지 브로커로 사용하기

1장에서 Pub/Sub · Stream · List 세 가지 메시징 패턴을 개괄했다면, 6장은 **각각의 동작 차이와 한계**를 판다. 특히 **클러스터에서 Pub/Sub이 왜 비효율적인지**, 이를 해결하는 **Sharded Pub/Sub(Redis 7.0+)**, 그리고 **List를 신뢰성 있는 큐로 쓰는 요령**이 핵심.

#### 목차

- [1부. Pub/Sub 동작 방식](#1부-pubsub-동작-방식)
- [2부. Stream 동작 방식](#2부-stream-동작-방식)
- [3부. Stream vs Pub/Sub 차이](#3부-stream-vs-pubsub-차이)
- [4부. Pub/Sub의 이슈](#4부-pubsub의-이슈)
- [5부. Sharded Pub/Sub](#5부-sharded-pubsub)
- [6부. List를 메시징 큐로 사용하기](#6부-list를-메시징-큐로-사용하기)
- [7부. 선택 가이드](#7부-선택-가이드)

---

## 1부. Pub/Sub 동작 방식

### 기본 명령

```
SUBSCRIBE news sports          # 채널 구독 (구독 모드 진입)
PSUBSCRIBE news.*              # 패턴 구독
PUBLISH news "hello"           # 발행 → 반환값 = 메시지를 받은 구독자 수
UNSUBSCRIBE / PUNSUBSCRIBE
PUBSUB CHANNELS [pattern]      # 활성 채널 목록
PUBSUB NUMSUB news             # 채널별 구독자 수
```

### 핵심 특성

- **Fire-and-Forget**: 서버는 메시지를 **저장하지 않음**. 발행 시점에 연결된 구독자에게 **즉시 push**하고 끝
- **At-most-once**: 구독자가 끊겨 있으면 그 메시지는 **영구 유실**, 재전송 없음
- **ACK 없음**: 구독자가 받았는지/처리했는지 서버는 모름
- **구독 모드 연결은 전용**: `SUBSCRIBE` 한 커넥션은 구독 관련 명령만 가능 → **발행용/구독용 커넥션 분리** 필요
- **Consumer Group 개념 없음**: 같은 채널 구독자는 **모두** 같은 메시지를 받음 (팬아웃). 작업 분산 불가
- **클라이언트 출력 버퍼 주의**: 구독자가 느리면 서버의 출력 버퍼가 쌓임 → `client-output-buffer-limit pubsub` (기본 `32mb 8mb 60`) 초과 시 **서버가 해당 구독자 연결을 강제 종료**

---

## 2부. Stream 동작 방식

### 기본 명령

```
XADD mystream * type "order" id 123      # 추가 (ID 자동: <ms>-<seq>)
XLEN mystream
XRANGE mystream - +                      # 범위 조회 (- = 최소, + = 최대)
XREAD COUNT 10 BLOCK 5000 STREAMS mystream $   # 신규 메시지 대기 읽기

# Consumer Group
XGROUP CREATE mystream grp $ MKSTREAM
XREADGROUP GROUP grp c1 COUNT 10 BLOCK 5000 STREAMS mystream >
XACK mystream grp <id>
XPENDING mystream grp                    # 미처리(PEL) 조회
XAUTOCLAIM mystream grp c2 60000 0-0     # 60초 넘게 처리 안 된 메시지 가져오기 (6.2+)
XTRIM mystream MAXLEN ~ 100000           # 길이 제한 (~ = 근사, 효율적)
```

### 핵심 특성

- **로그 자료 구조**: 메시지가 **ID와 함께 영속적으로 쌓임** (RDB/AOF에도 포함)
- **Consumer Group**: 그룹 내 컨슈머들이 메시지를 **나눠서** 처리 (작업 분산), 그룹이 여러 개면 각 그룹이 **전체를 독립적으로** 소비 (팬아웃)
- **PEL(Pending Entries List)**: 컨슈머에게 전달됐지만 **ACK 안 된 메시지** 목록 → 컨슈머 장애 시 `XAUTOCLAIM`/`XCLAIM`으로 **재할당**
- **At-least-once**: ACK 전 장애 → 재처리 → 중복 가능 → **멱등 처리 필요**
- **메모리 상주**: 로그가 계속 쌓이므로 **`MAXLEN`/`MINID`로 트리밍 필수**

---

## 3부. Stream vs Pub/Sub 차이

| 축 | Pub/Sub | Stream |
|---|---|---|
| **메시지 저장** | ❌ (전달 후 소멸) | ✅ (로그로 보관, 영속화 대상) |
| **전달 보장** | at-most-once | **at-least-once** (ACK + PEL) |
| **구독자 오프라인 시** | 메시지 **유실** | 복귀 후 **이어서 읽기** (마지막 ID부터) |
| **과거 메시지 조회** | ❌ | ✅ (`XRANGE`, ID 기반 재생) |
| **소비 모델** | 모든 구독자에 **팬아웃** | 그룹 내 **분산(load balancing)** + 그룹 간 팬아웃 |
| **ACK** | ❌ | ✅ (`XACK`) |
| **장애 복구** | 없음 | `XPENDING` / `XAUTOCLAIM` |
| **패턴 구독** | ✅ (`PSUBSCRIBE`) | ❌ (키 단위) |
| **지연** | 매우 낮음 | 낮음 (약간 더 큼) |
| **메모리** | 거의 안 씀 | 쌓인 만큼 사용 (트리밍 필요) |
| **클러스터** | 전체 노드 브로드캐스트 (비효율) | **일반 키처럼 슬롯 단위** 샤딩 |
| **적합 용도** | 실시간 알림, 캐시 무효화 신호 | 작업 큐, 이벤트 로그, 유실 불가 메시지 |

### 한 줄 기준

> **"유실돼도 괜찮은 실시간 신호"는 Pub/Sub, "반드시 처리돼야 하는 메시지"는 Stream.**

### 선택 예시

| 시나리오 | 선택 | 이유 |
|---|---|---|
| 서버 간 로컬 캐시 무효화 신호 | Pub/Sub | 놓쳐도 TTL로 수렴, 지연이 중요 |
| 채팅 실시간 푸시 (히스토리는 DB) | Pub/Sub | 접속 중인 사용자에게만 전달 |
| 주문 접수 → 결제/재고 처리 | Stream | 유실 불가, 재처리 필요 |
| 이메일 발송 워커 풀 | Stream (또는 List) | 작업 분산 + 실패 재시도 |
| 사용자 행동 이벤트 로그 | Stream | 재생·다중 컨슈머 그룹 |

---

## 4부. Pub/Sub의 이슈

### 이슈 1. 클러스터 모드에서의 브로드캐스트 증폭

일반 `PUBLISH`는 **클러스터에서 채널이 슬롯에 매핑되지 않는다**. 어느 노드에 `PUBLISH`하든, 그 노드가 **Cluster Bus(gossip 채널)로 클러스터의 모든 노드에 메시지를 전파**하고, 각 노드가 자기에게 붙은 구독자에게 전달한다.

```
Client ──PUBLISH ch "msg"──> Node A (Master 1)
                                │
              Cluster Bus 브로드캐스트 (모든 노드에게)
        ┌───────────┬───────────┼───────────┬───────────┐
        ▼           ▼           ▼           ▼           ▼
    Replica A'   Master 2    Replica 2'   Master 3    Replica 3'
   (구독자 있으면 각자 전달)
```

**문제점**:
- 메시지가 **구독자 유무와 상관없이 모든 노드(마스터 + 레플리카)에 전송**됨 → 구독자가 한 노드에만 있어도 전체로 퍼짐
- 클러스터 내부 트래픽이 **노드 수에 비례해 증폭** — 책의 예시처럼 마스터 1대 + 레플리카가 붙은 구성이면 같은 메시지가 레플리카 수만큼 추가로 복제되어 **트래픽이 몇 배**로 늘어남 (노드가 N개면 발행 1건당 **N-1번 전송**)
- **노드를 늘려도(스케일 아웃) Pub/Sub 처리량이 늘지 않고 오히려 클러스터 버스 부하가 커짐** → 수평 확장이 안 되는 구조
- Cluster Bus는 **노드 상태 관리(gossip, failover 감지)와 같은 채널**을 공유 → 메시지가 폭주하면 **클러스터 안정성(heartbeat 지연 → 오탐 failover)**까지 위협

> 참고: 이 증폭은 레플리카 동기화(replication)와는 **별개**. 복제는 데이터셋 변경을 따라가는 것이고, 여기서는 **Pub/Sub 메시지 자체를 모든 노드에 뿌리는** 클러스터 버스 브로드캐스트가 원인. 정확한 배수는 **클러스터 노드 수**에 따라 달라지며, "×3"은 3노드 구성 예시로 이해하는 게 맞음.

### 이슈 2. 메시지 유실 (at-most-once)

- 구독자 재접속 중, 네트워크 순단, 서버 재시작 시 **메시지가 사라짐**
- 서버가 기록을 안 남기므로 **사후 추적 불가**

### 이슈 3. 느린 구독자 → 연결 강제 종료

- 구독자가 처리를 못 따라가면 출력 버퍼 누적 → `client-output-buffer-limit pubsub` 초과 시 **연결 kill** → 이후 메시지 유실
- 모니터링: `INFO clients`의 `client_recent_max_output_buffer`

### 이슈 4. 패턴 구독 비용

- `PSUBSCRIBE`는 **발행마다 모든 패턴과 매칭** 검사 → 패턴 수 × 발행량에 비례해 CPU 증가
- 패턴 구독자가 많은 환경에서 지연 증가

### 이슈 5. 부하 분산 불가

- 같은 채널의 모든 구독자가 **같은 메시지를 중복 수신** → 워커 풀 분산 처리 불가 → 작업 큐 용도로 부적합

---

## 5부. Sharded Pub/Sub

**Redis 7.0+**. 4부의 클러스터 브로드캐스트 문제를 해결하기 위한 기능.

### 핵심 아이디어

> **채널을 키처럼 취급해 해시 슬롯에 매핑**하고, 메시지를 **그 슬롯을 소유한 샤드(마스터 + 그 레플리카) 안에서만** 전파한다.

```
슬롯 = CRC16(channel) mod 16384      ← 키와 동일한 방식

Client ──SPUBLISH ch "msg"──> (슬롯 소유 샤드)
                                 │
                  샤드 내부(마스터 → 자기 레플리카)에만 전파
                                 ▼
                  해당 샤드에 SSUBSCRIBE한 구독자들에게 전달

다른 샤드의 노드들에게는 전송 X
```

### 명령어

| 일반 | Sharded | 설명 |
|---|---|---|
| `SUBSCRIBE` | **`SSUBSCRIBE`** | 샤드 채널 구독 |
| `UNSUBSCRIBE` | **`SUNSUBSCRIBE`** | 구독 해제 |
| `PUBLISH` | **`SPUBLISH`** | 샤드 채널 발행 |
| `PUBSUB CHANNELS` | `PUBSUB SHARDCHANNELS` | 활성 샤드 채널 |
| `PUBSUB NUMSUB` | `PUBSUB SHARDNUMSUB` | 채널별 구독자 수 |

```
SSUBSCRIBE orders:123
SPUBLISH orders:123 "paid"
```

### 개선되는 점

| 축 | 일반 Pub/Sub (클러스터) | Sharded Pub/Sub |
|---|---|---|
| **전파 범위** | **모든 노드** | **해당 슬롯의 샤드(마스터+레플리카)만** |
| **클러스터 버스 부하** | 노드 수에 비례해 증폭 | 샤드 내부로 한정 |
| **수평 확장** | ❌ (노드 추가 효과 없음) | ✅ (샤드가 늘면 채널이 분산) |
| **레플리카 활용** | 모든 노드가 수신 | 샤드의 **레플리카에서도 구독 가능** → 구독 부하 분산 |
| **슬롯 이동(리샤딩) 시** | 영향 없음 | 구독자에게 **`sunsubscribe` 알림**이 와서 재구독 필요 |

### 사용 시 제약 / 주의

1. **패턴 구독 미지원**: `SPSUBSCRIBE` 같은 건 없음 (채널을 정확히 알아야 슬롯 계산 가능)
2. **같은 `SSUBSCRIBE`에 여러 채널을 넣으면 모두 같은 슬롯**이어야 함 (`CROSSSLOT` 에러) → 필요하면 **해시태그** `{room1}:chat`, `{room1}:typing` 사용
3. **리샤딩/failover 시 재구독 로직 필요**: 슬롯이 다른 노드로 이동하면 서버가 구독을 끊고 `sunsubscribe` push → **클라이언트 라이브러리가 재구독을 지원**해야 함 (Lettuce, Jedis, redis-py 등 지원 버전 확인)
4. **전달 보장은 여전히 없음** — 저장 안 하는 Fire-and-Forget 특성은 동일 (at-most-once)
5. **채널 분포 설계**: 채널이 소수면 특정 샤드에 쏠림(Hot shard). 채널 이름을 세분화(`orders:{id}`)해 슬롯을 고르게 분산
6. **단일 노드(비클러스터)**에서는 일반 Pub/Sub과 사실상 차이 없음 — **클러스터 전용 개선**

### 어떻게 선택하나

| 환경 | 선택 |
|---|---|
| 단일 인스턴스 / Sentinel | 일반 `PUBLISH` |
| **Cluster + Pub/Sub 사용량 많음** | **Sharded Pub/Sub** |
| Cluster + 유실 불가 | Stream (키 단위로 슬롯 분산) |

---

## 6부. List를 메시징 큐로 사용하기

### 기본 패턴

```
# Producer
LPUSH queue "job1"

# Consumer (블로킹)
BRPOP queue 0          # 0 = 무한 대기. 비면 대기, 생기면 즉시 반환
```

- `LPUSH` + `BRPOP` → **FIFO**
- **O(1)** 푸시/팝, 구현·운영이 가장 단순
- 컨슈머가 여러 개여도 **한 메시지는 한 컨슈머에게만** 전달 → 자연스러운 **작업 분산**

### 핵심 한계 — 유실 가능성

`BRPOP`은 **꺼내는 순간 리스트에서 제거**된다. 컨슈머가 꺼낸 뒤 처리 중 죽으면 **메시지 영구 유실**. Pub/Sub보다는 낫지만 (큐에 남아 있다가 꺼내지므로) **처리 보장은 없음**.

### 팁 1. 신뢰성 큐 — `BLMOVE` (Reliable Queue)

꺼내면서 **processing 리스트로 원자적 이동**.

```
# 꺼내서 processing에 보관 (원자적)
BLMOVE queue processing RIGHT LEFT 0      # 6.2+ (구 BRPOPLPUSH는 deprecated)

# 처리 성공 → processing에서 제거 (ACK 역할)
LREM processing 1 "job1"

# 컨슈머 장애 → processing에 남은 작업을 queue로 되돌림 (복구 워커)
LMOVE processing queue LEFT RIGHT
```

```
queue ──BLMOVE──> processing ──(성공)──> LREM
                       │
                       └─(장애/타임아웃)──> queue로 재투입
```

**보완 사항**:
- processing에 **언제 들어갔는지** 모르면 "정말 죽은 건지 처리 중인지" 구분 불가 → **작업 ID + 시작 시각을 Hash/ZSet에 별도 기록**해 **타임아웃(visibility timeout)** 구현
- 재시도 횟수 제한 + **Dead Letter 리스트**로 이동 (무한 재시도 방지 — poison message 대응)

### 팁 2. 우선순위 큐 — 다중 키 `BRPOP`

```
BRPOP queue:high queue:normal queue:low 0
```

- **앞쪽 키부터 순서대로** 확인해서 먼저 비어있지 않은 큐에서 pop
- high가 계속 차 있으면 low는 **기아(starvation)** 가능 → 가중치가 필요하면 별도 로직

### 팁 3. 블로킹 커넥션 관리

- `BRPOP`/`BLMOVE`는 **커넥션을 점유** → 일반 명령과 **커넥션 분리**, 커넥션 풀에서 블로킹 용도 풀 별도 운영
- `BRPOP ... 0` (무한)이면 **네트워크 단절 감지가 어려움** → 타임아웃을 5~30초 정도로 두고 **루프 재진입**
- 서버 `timeout` 설정(유휴 커넥션 종료)과 충돌하지 않게 주의 (블로킹 대기 중에는 유휴로 판단되지 않지만, 프록시/LB의 idle timeout은 별개)
- 클라이언트 라이브러리의 **read timeout > BRPOP timeout**이어야 함 (아니면 정상 대기 중 타임아웃 에러)

### 팁 4. 여러 개 한 번에 — `LMPOP` / `BLMPOP` (7.0+)

```
BLMPOP 5 2 queue:a queue:b LEFT COUNT 10    # 최대 10개를 한 번에
```

- 배치 처리로 **왕복 횟수 절감**

### 팁 5. 큐 길이 보호

```
# 생산 속도가 소비보다 빠를 때 메모리 폭주 방지
LPUSH queue "job"
LTRIM queue 0 99999          # 상한 유지 (오래된 것 버림 — 유실 허용 시)
LLEN queue                   # 길이 모니터링 → 알람
```

- 유실 불가면 `LTRIM` 대신 **LLEN 임계치 초과 시 생산자에 backpressure** 적용

### 팁 6. 큐 하나에 거대한 값 금지

- 메시지에는 **ID/포인터만** 넣고 본문은 DB/S3에 두기 → Big Key 방지, 메모리 절약

### 팁 7. 멱등성 설계

- 재투입/재시도 구조에서는 **같은 작업이 두 번 실행될 수 있음** → 작업 ID 기반 **중복 실행 방지**(예: `SET done:{id} 1 NX`)

### List 큐 vs Stream 비교

| 축 | List 큐 | Stream |
|---|---|---|
| **구현 난이도** | 매우 쉬움 | 중간 |
| **ACK / 재처리** | 직접 구현 (BLMOVE + 보조 구조) | **내장** (PEL, XACK, XAUTOCLAIM) |
| **Consumer Group** | ❌ | ✅ |
| **여러 그룹이 같은 메시지 소비** | ❌ (pop하면 사라짐) | ✅ |
| **과거 메시지 조회/재생** | ❌ | ✅ |
| **메시지 단위 메타데이터** | ❌ | ✅ (ID, 필드) |
| **메모리 효율** | 높음 | 약간 낮음 |
| **적합** | 단순 작업 큐, 소규모 | 신뢰성·확장성 필요한 큐/이벤트 |

→ **"그냥 백그라운드 작업 몇 개 돌리고 싶다"는 List, "유실·재처리·다중 소비자가 중요하다"는 Stream.** 요구가 커져서 List 위에 ACK/타임아웃/DLQ를 직접 쌓고 있다면 Stream으로 갈아탈 시점이다.

---

## 7부. 선택 가이드

| 요구사항 | 선택 |
|---|---|
| 실시간 알림, **유실 허용**, 최소 지연 | Pub/Sub (클러스터면 **Sharded Pub/Sub**) |
| **유실 불가**, 재처리, 컨슈머 그룹 | **Stream** |
| 단순 작업 큐, 구현 단순함 우선 | List (`LPUSH` + `BRPOP`) |
| 단순 큐 + 장애 시 재처리 | List + `BLMOVE` (reliable queue) |
| 대규모 영구 이벤트 스트림, 장기 보관 | Kafka (Redis 범위 초과) |

---

### 요약

- **Pub/Sub**: 저장 없는 Fire-and-Forget, at-most-once, 전체 구독자 팬아웃. 오프라인 구독자는 메시지 유실
- **Stream**: 로그 + Consumer Group + ACK/PEL, at-least-once. 영속·재생·분산 처리 가능. `MAXLEN` 트리밍 필수
- **Pub/Sub 이슈**: 클러스터에서 `PUBLISH`가 **모든 노드(마스터+레플리카)에 브로드캐스트** → 노드 수에 비례해 클러스터 버스 트래픽 증폭, 스케일 아웃이 안 됨. 그 외 유실, 느린 구독자 강제 종료, 패턴 구독 비용
- **Sharded Pub/Sub (7.0+)**: 채널을 **슬롯에 매핑**해 **해당 샤드 안에서만 전파** (`SSUBSCRIBE`/`SPUBLISH`). 패턴 구독 불가, 멀티 채널은 같은 슬롯 필요, 리샤딩 시 재구독 처리 필요, 전달 보장은 여전히 없음
- **List 큐**: `LPUSH` + `BRPOP`이 기본. 유실 방지는 **`BLMOVE` + processing 리스트**, 우선순위는 **다중 키 BRPOP**, 블로킹 커넥션 분리·타임아웃 설정, 멱등성 필수
- **선택 기준**: 유실 허용 신호 → Pub/Sub / 유실 불가·재처리 → Stream / 단순 작업 큐 → List
