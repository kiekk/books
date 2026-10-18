### CHAPTER 12 클라이언트 관리

Redis 서버는 **싱글 스레드(명령 실행)** 이므로, 서버 한 대가 감당하는 것은 "명령 처리 속도" 만이 아니라 **수천 개 클라이언트가 붙들고 있는 버퍼(메모리)** 와 **클라이언트와의 왕복(RTT) 비용** 이기도 하다. 이 장은 세 가지를 다룬다.

1. **클라이언트 버퍼와 이빅션** — 느린 클라이언트/거대한 요청이 서버 메모리를 잠식하는 문제와 방어 설정
2. **파이프라이닝** — 왕복 횟수를 줄여 처리량을 올리는 방법
3. **클라이언트 사이드 캐싱** — 서버에 묻지 않고 **클라이언트 로컬 메모리(L1)** 에서 읽기

> 이 장의 동작은 **로컬 Redis 7.2.6** 에서 직접 확인했다. 확인한 부분은 `✅ 검증`, 문서/지식 기준은 `(문서 기준)` 으로 표시했다. 성능 수치는 **단일 호스트(loopback) 측정값**으로, 실제 네트워크에서는 RTT 가 더 커서 파이프라인 효과가 **더 크게** 나타난다.

#### 목차

- [1부. 클라이언트 버퍼와 이빅션](#1부-클라이언트-버퍼와-이빅션)
- [2부. 파이프라이닝](#2부-파이프라이닝)
- [3부. 클라이언트 사이드 캐싱](#3부-클라이언트-사이드-캐싱)
- [4부. 클라이언트 관리 설정 한눈에 보기](#4부-클라이언트-관리-설정-한눈에-보기)

---

## 1부. 클라이언트 버퍼와 이빅션

### 1-1. 클라이언트 한 명당 서버가 쓰는 메모리

```
 클라이언트 ──요청(RESP)──▶ [query buffer(qbuf)]  ── 파싱 ──▶ 명령 실행
            ◀─응답───────  [output buffer(obl/oll/omem)] ◀── 결과
```

| 버퍼 | 설명 | `CLIENT LIST` 필드 |
|---|---|---|
| **Query buffer (입력)** | 클라이언트가 보낸 **아직 처리 안 된 요청 바이트**. 큰 값 `SET`·파이프라인·느린 전송으로 쌓임 | `qbuf`, `qbuf-free` |
| **Output buffer (출력)** | 서버가 **아직 클라이언트에게 못 보낸 응답**. 고정 16KB **reply buffer(`obl`)** + 넘치면 **reply list(`oll` 개 청크)** | `obl`(고정 버퍼 크기), `oll`(리스트 길이), `omem`(리스트 메모리) |
| 기타 | 인자 파싱용(`argv-mem`), MULTI 큐(`multi-mem`), Pub/Sub 구독 정보 등 | `argv-mem`, `multi-mem`, `tot-mem`(합계) |

✅ 확인(7.2.6 `CLIENT LIST` 필드): `qbuf=26 qbuf-free=16864 argv-mem=10 multi-mem=0 rbs=16384 rbp=16384 obl=0 oll=0 omem=0 tot-mem=34090` — **유휴 연결 1개가 약 34KB**(= 연결 1만 개면 약 340MB 가 기본 오버헤드).

> **핵심**: 응답은 서버가 **먼저 만들어 두고** 클라이언트가 읽어가는 속도에 맞춰 흘려보낸다. **클라이언트가 느리거나 읽지 않으면 응답이 서버 메모리에 쌓인다.** 서버는 이를 **클라이언트가 아니라 자기 메모리로 부담**한다.

### 1-2. Output buffer limit — `client-output-buffer-limit`

```
client-output-buffer-limit <class> <hard-limit> <soft-limit> <soft-seconds>
```

기본값 ✅ 확인:

| class | hard | soft | soft 초 | 의미 |
|---|---|---|---|---|
| **normal** (일반 클라이언트) | **0** | **0** | **0** | **무제한(제한 없음)** |
| **replica** (레플리카 연결) | 256mb | 64mb | 60 | 복제 지연 시 마스터 보호 |
| **pubsub** (구독 클라이언트) | 32mb | 8mb | 60 | 느린 구독자 보호 |

- **hard limit**: 버퍼가 이 크기에 **도달하면 즉시 연결 종료**
- **soft limit**: **soft 초 동안 계속** soft limit 이상을 유지하면 연결 종료(일시적 폭증은 허용)
- `0` = 해당 제한 없음
- ⚠️ **normal 이 무제한이 기본**이므로, **`GET` 이 큰 값을 반환하는 느린/죽은 클라이언트** 가 서버 메모리를 끝없이 키울 수 있다(OOM / 큰 maxmemory 도달 → eviction 증가).

✅ 검증 — 느린 클라이언트 강제 종료 (`normal 1mb 0 0`, `pubsub 1mb 512kb 5`):
```
# 100KB 값에 대한 GET 500개를 보내고 응답을 읽지 않음 (수신 버퍼 4KB)
CLIENT LIST: obl=16384 oll=11 omem=1114376      (≈1.06MB > hard 1mb)
로그: Client id=16 ... cmd=get ... scheduled to be closed ASAP for overcoming of output buffer limits.
→ connected_clients 가 1 로 감소 (해당 연결 종료)

# 구독자가 읽지 않는 동안 50KB 메시지 60건 PUBLISH
로그: Client id=19 ... flags=P sub=1 ... cmd=subscribe ... omem=1065464 ... scheduled to be closed ASAP ...
PUBSUB NUMSUB ch → 0     (구독자 제거됨)
```
- **종료 대상 클라이언트는 연결이 끊긴 것을 모른 채** 명령 실패/`Connection reset` 을 맞는다 → 클라이언트는 **재연결·재구독** 로직 필요
- 로그에 `omem`/`oll` 이 찍혀 **원인 클라이언트(`cmd=`, `addr=`)** 를 바로 알 수 있다
- **레플리카 class**: 마스터 쓰기량이 많고 레플리카가 느리면 **256mb 초과로 연결이 끊기고 재동기화 → 다시 끊기는 sync 루프**(8장). 7.0+ 는 레플리카 버퍼를 **공유 복제 버퍼(shared replication buffer)** 로 통합해 메모리를 줄였지만 `limit` 은 여전히 적용

### 1-3. Query buffer — `client-query-buffer-limit`, `proto-max-bulk-len`

| 설정 | 기본값 | 의미 |
|---|---|---|
| `client-query-buffer-limit` | **1gb** ✅ | 클라이언트 **요청 버퍼 상한**. 초과 시 **연결 종료** |
| `proto-max-bulk-len` | **512mb** ✅ | **단일 인자(bulk string) 최대 크기**. 초과 요청은 프로토콜 에러 |

✅ 검증:
```
client-query-buffer-limit 1mb 로 낮추고 3MB 값을 천천히 전송
→ 로그: Closing client that reached max query buffer length: id=82 ...
→ 클라이언트는 BrokenPipe / ConnectionReset

proto-max-bulk-len 1mb 로 낮추고 $2000000 (2MB) 선언
→ -ERR Protocol error: invalid bulk length
```
- **파이프라인으로 대량 명령을 한꺼번에** 보내면 query buffer 가 커진다 → 너무 큰 파이프라인(2부)은 **서버 메모리/limit** 에 걸릴 수 있음
- 1gb 기본값은 사실상 "보호 없음" → 환경에 맞게 낮추거나 아래 `maxmemory-clients` 로 총량 제한

### 1-4. 연결 수 제한과 유휴 연결

| 설정 | 기본값 | 의미 |
|---|---|---|
| **`maxclients`** | **10000** ✅ | 동시 연결 상한. 초과 시 **`-ERR max number of clients reached`** 반환 후 종료. 실제 상한은 **OS 파일 디스크립터(`ulimit -n`)** 로 제한됨 (`maxclients` > `ulimit -n - 32` 이면 서버가 줄여서 기동) |
| `timeout` | **0** ✅ | **유휴 연결 자동 종료 시간(초)**. 0 = 끊지 않음 |
| `tcp-keepalive` | **300** ✅ | TCP keepalive 주기(초). 끊긴 클라이언트(방화벽 타임아웃 등) 정리 |
| `tcp-backlog` | 511 | listen 큐 길이(`somaxconn` 도 함께 조정) |

✅ 검증: `--maxclients 3` 로 기동 → 연결 6개 시도 → **앞 3개 `+PONG`, 뒤 3개 `-ERR max number of clients reached`**, `INFO stats` 의 **`rejected_connections:3`**
(참고: 같은 서버에 **런타임 `CONFIG SET maxclients 3`** 을 한 뒤 연결 6개를 시도했을 때는 **전부 PONG** 을 받았다. 재현은 했으나 **원인은 확인하지 못함**.)

- 연결 폭주 방지의 정석은 **커넥션 풀(상한 설정)** + `maxclients` 는 안전판. **풀 크기 × 앱 인스턴스 수** 가 `maxclients` 를 넘지 않도록 계산(오토스케일링 시 특히 주의)
- 방화벽/NAT/LB 가 유휴 TCP 를 끊는 환경: 클라이언트 쪽 **keepalive/`PING` 주기**, 서버 `timeout` 은 신중히

### 1-5. 클라이언트 이빅션 (Client Eviction) — `maxmemory-clients` (7.0+)

**모든 클라이언트가 쓰는 메모리 총합이 한도를 넘으면, 메모리를 가장 많이 쓰는 클라이언트부터 연결을 끊는 기능.** (키 이빅션 `maxmemory-policy` 와 **별개**)

```
maxmemory-clients 0        # 기본값: 비활성 ✅ 확인
maxmemory-clients 1gb      # 절대값
maxmemory-clients 10%      # maxmemory 의 10%
```

- 대상: **`tot-mem`(query + output + argv 등)** 이 큰 클라이언트부터 **연결 종료** (`CLIENT LIST` 의 flags `e`)
- 제외: **`CLIENT NO-EVICT on`** 이 설정된 클라이언트(주로 **운영/모니터링/복제 연결**), 마스터 연결 등
- 효과: `client-output-buffer-limit normal` 이 무제한이어도 **개별이 아니라 "전체"** 가 한도를 넘으면 가장 큰 쪽부터 정리 → **OOM 방어**

✅ 검증 (`maxmemory-clients 2mb`, output buffer 제한 없음, 2개 클라이언트가 100KB 값 `GET` 200건을 읽지 않음, 한쪽만 `CLIENT NO-EVICT ON`):
```
{'id':'100','flags':'e', 'omem':'19239424', 'tot-mem':'19258128'}   ← NO-EVICT 클라이언트: 19MB 를 쓰는데도 유지됨 (flags e)
(일반 클라이언트는 사라짐)   INFO stats: evicted_clients:3
로그: Evicting client: id=103 ...
      # Over client maxmemory after evicting all evictable clients       ← 이빅션 대상이 더 없는데도 한도 초과 상태
```
- **NO-EVICT 는 한도를 무시하고 메모리를 계속 쓴다** → 신뢰하는 클라이언트에만 설정
- `INFO stats` 의 **`evicted_clients`** 를 모니터링해 **어떤 앱이 큰 응답을 쌓는지** 파악

### 1-6. 키 이빅션(`maxmemory-policy`)과의 관계 (5장 연결)

| 구분 | 키 이빅션 | 클라이언트 이빅션 |
|---|---|---|
| 설정 | `maxmemory`, `maxmemory-policy` | `maxmemory-clients`, `client-output-buffer-limit` |
| 대상 | **데이터(키)** | **연결(클라이언트)** |
| 결과 | 키 삭제(캐시 miss) | **연결 종료**(요청 실패, 재연결 필요) |

- `used_memory` 에는 **클라이언트 버퍼도 포함**된다 → **느린 클라이언트의 큰 output buffer 가 `maxmemory` 에 도달하면 키가 지워진다**(데이터 손실처럼 보임). 단 **레플리카 출력/AOF 버퍼는 eviction 계산에서 제외**(`mem_not_counted_for_evict`) (문서 기준)
- `INFO memory`: `mem_clients_normal`, `mem_clients_slaves`, `mem_not_counted_for_evict`, `used_memory` 로 비중 확인
- 점검 명령: `CLIENT LIST` (`omem` 큰 순 정렬), `INFO clients` (`connected_clients`, `blocked_clients`, `client_recent_max_input_buffer`, `client_recent_max_output_buffer`), `CLIENT KILL ID/ADDR/USER/MAXAGE`

### 1-7. 대응 가이드

| 증상 | 원인 | 대응 |
|---|---|---|
| 메모리가 서서히 증가, 재시작해야 줄어듦 | 느린/죽은 클라이언트의 **output buffer 누적** | `normal` limit 설정(예: `normal 256mb 128mb 60`), `maxmemory-clients`, 원인 클라이언트 `CLIENT KILL` |
| `MONITOR` 사용 후 메모리/CPU 급증 | `MONITOR` 클라이언트가 **모든 명령을 응답 버퍼로 수신** | 운영 중 장시간 사용 금지 |
| 대량 `SMEMBERS`/`HGETALL`/`LRANGE 0 -1` 후 메모리 스파이크 | 큰 응답이 output buffer 에 적재 | `SSCAN`/`HSCAN`/페이지네이션 |
| Pub/Sub 구독자가 자꾸 끊김 | `pubsub` limit 초과(구독 처리 느림/메시지 큼) | 구독자 처리 속도 개선, 메시지 크기 축소, limit 조정, 유실 불가면 **Stream**(6장) |
| 복제가 반복해서 끊김(full sync 반복) | `replica` limit 초과 | `replica` limit 상향·`repl-backlog-size`·쓰기량 완화(8장) |
| `max number of clients reached` | 연결 풀 과다/연결 누수 | 풀 상한, `CLIENT LIST` 로 `idle` 큰 연결 점검, `timeout` |

---

## 2부. 파이프라이닝

### 2-1. 개념 — 왜 필요한가

Redis 는 **요청-응답** 프로토콜이다. 한 명령마다 **클라이언트 → 서버 → 클라이언트** 한 번의 왕복(**RTT**)을 기다린다. 명령 처리 자체는 마이크로초지만, **네트워크 RTT(같은 AZ 약 0.1~1ms, 리전 간 수십 ms)** 가 대부분을 차지한다.

```
[일반]   → SET a   ← OK  → SET b   ← OK  → SET c   ← OK       RTT × 3
[파이프]  → SET a SET b SET c  (한 번에 전송)  ← OK OK OK        RTT × 1
```

- **파이프라이닝 = 응답을 기다리지 않고 여러 명령을 연달아(또는 한 덩어리로) 보내고, 응답을 나중에 한꺼번에 읽는 것**
- 서버 쪽 이득: 한 번의 `read()` 로 여러 명령을 읽고 한 번의 `write()` 로 응답을 보내 **시스템콜(컨텍스트 스위치) 횟수가 줄어듦** → RTT 가 0 에 가까운 환경(loopback)에서도 **서버 처리량이 크게 증가**
- **명령은 순서대로 실행**되고 **응답도 같은 순서**로 온다(순서 보장). 단 **원자성은 없다**

### 2-2. 동작 원리

1. 클라이언트가 명령 N 개를 **소켓 송신 버퍼에 연속 기록**(한 번 `flush`)
2. 서버는 **query buffer** 에 읽어 **한 개씩 순차 실행**, 응답을 **output buffer** 에 **누적**
3. 이벤트 루프가 **응답을 모아서 한 번에 `write`**
4. 클라이언트는 N 개의 응답을 **순서대로 읽어 요청과 매칭**

→ 클라이언트가 **응답을 읽는 속도**에 따라 서버 output buffer 가 커짐(1부). **한 파이프라인에 너무 많이 담지 않는 것**이 중요.

### 2-3. 성능 측정 ✅ 검증 (loopback, 단일 클라이언트 `-c 1`, `redis-benchmark`)

| 파이프라인 크기(-P) | SET (req/s) | GET (req/s) | p50 지연 |
|---|---|---|---|
| 1 (없음) | 약 14,000 | 약 15,400 | 0.063 ms |
| 10 | 약 144,000 | 약 150,000 | 0.07 ms |
| 100 | 약 893,000 | 약 980,000 | 0.10 ms |
| 1000 | 약 1,650,000 | 약 2,060,000 | 0.22 ms |

- **P=1 → 1000 에서 처리량 약 100배+**. 단, **p50 지연(한 배치의 완료 시간)** 은 배치가 커질수록 증가 → **처리량과 개별 지연의 트레이드오프**
- 직접 구현 소켓 테스트(20,000 `SET`): **비파이프라인 1.29초(15,500/s) vs 파이프라인 0.014초(1,414,700/s) → 약 91배**
- `redis-cli` 대량 입력 비교(10만 `SET` 파일): **일반 입력 6.6초 vs `redis-cli --pipe` 0.107초 → 약 60배**, `errors: 0, replies: 100000`
- ⚠️ 위는 **loopback(RTT≈0)** 값이다. **RTT 가 0.5ms 이상**이면 비파이프라인은 **초당 2,000건 이하**로 떨어져 파이프라인 효과는 훨씬 극적이다.

### 2-4. 파이프라인 vs 트랜잭션 vs Lua vs 멀티 키 명령

| 구분 | 파이프라이닝 | `MULTI/EXEC` | Lua/Function | `MSET`/`MGET` |
|---|---|---|---|---|
| 목적 | **RTT 절감** | **원자적 실행** | **원자 + 서버측 로직** | 한 명령으로 여러 키 |
| 원자성 | ❌ (중간에 다른 클라이언트 명령이 끼어듦) | ✅ (EXEC 시 연속 실행, 롤백은 없음) | ✅ | ✅ (단일 명령) |
| 명령 간 의존 | ❌ (응답을 모르고 다음 명령을 보냄) | ❌ (큐잉만) | ✅ (스크립트 내부) | ❌ |
| 부분 실패 | **명령별 독립 응답**(일부만 에러 가능) | 큐잉 시 문법 오류면 EXEC 전체 취소, 실행 중 오류는 해당 명령만 | 스크립트 오류 시 중단 | 단일 명령 |
| 클러스터 | **슬롯별로 분리** 필요 | **같은 슬롯**만 | 같은 슬롯만 | 같은 슬롯만 |

- **조합**: 파이프라인 안에 `MULTI … EXEC` 를 넣어 **"RTT 1회 + 원자성"** 을 함께 얻을 수 있다(Spring `executePipelined` + `multi()`, 라이브러리별 지원).
- 이전 명령의 **결과에 따라 다음 명령이 달라지는 로직**은 파이프라인으로 못 한다 → Lua/Function 또는 `WATCH` 낙관적 락.

### 2-5. 실무 주의사항

| 항목 | 설명 |
|---|---|
| **배치 크기** | 한 번에 수만~수십만 건은 서버 **query buffer / output buffer 메모리 급증**. 공식 문서 권장: **약 10,000 명령 단위로 끊어** 전송 |
| **응답 처리 지연** | 응답을 읽지 않으면 서버 output buffer 증가 → 1부 limit 에 걸림. **쓰기와 읽기를 병행(비동기)** 하는 클라이언트 라이브러리가 안전 |
| **명령별 에러** | 한 명령이 실패해도 **나머지는 정상 실행**. 반드시 **각 응답을 검사** |
| **순서/매칭** | 응답 순서 = 요청 순서. 라이브러리가 내부 큐로 매칭 |
| **원자성 오해** | 파이프라인은 **트랜잭션이 아님**. 중간에 다른 클라이언트의 쓰기가 끼어듦 ✅ (명령별 개별 실행, MULTI 없음) |
| **대량 입력(마이그레이션/시드)** | **`redis-cli --pipe`** (RESP 로 인코딩한 파일을 흘려보내고 끝에 **echo 마커로 응답 수 검증**) ✅ |
| **클러스터** | 키가 여러 슬롯에 흩어지므로 **라이브러리가 노드별로 쪼개** 전송(Lettuce/Jedis Cluster). 해시태그 없이는 **한 번의 RTT 가 아님**(노드 수만큼 병렬) |
| **블로킹 명령** | `BLPOP` 등을 파이프라인에 섞으면 뒤 명령이 지연 → 분리 |
| **`SELECT`/`AUTH`/`CLIENT` 상태** | 연결 단위 상태이므로 파이프라인 중간에서 바뀌면 이후 명령에 영향 |
| **느린 명령 혼입** | `KEYS`, 큰 `SMEMBERS` 하나가 **뒤 명령 응답 전체를 지연**(싱글 스레드) |

### 2-6. 클라이언트 라이브러리에서

- **Lettuce**: 비동기 API 가 기본적으로 **자동 파이프라이닝**(공유 커넥션에서 명령이 연속 전송). `setAutoFlushCommands(false)` + `flushCommands()` 로 **수동 배치**
- **Jedis**: `Pipeline p = jedis.pipelined(); p.set(...); p.sync();`
- **Spring Data Redis**: `redisTemplate.executePipelined(SessionCallback)` (**콜백 안에서 읽은 값은 사용 불가**, 결과 리스트로 한 번에 반환)
- **redis-py**: `pipe = r.pipeline(transaction=False)`(기본 `True` 면 MULTI/EXEC 로 묶임)
- (라이브러리 동작은 문서 기준이며 이 환경에서 실행해보지 않음)

---

## 3부. 클라이언트 사이드 캐싱

### 3-1. 개념

**자주 읽는 데이터를 클라이언트(애플리케이션) 프로세스의 로컬 메모리에 저장**해, **네트워크 왕복 없이 읽는** 기법. 로컬 캐시를 **L1**, Redis 를 **L2** 로 보는 2단계 캐시 구조가 된다.

```
 [App]  ── L1 (로컬 메모리: Caffeine / Guava / HashMap)  ← 나노~마이크로초, 네트워크 0
          │ miss
          └─ L2 (Redis)                                  ← 밀리초 (RTT)
                 │ miss
                 └─ DB
```

| 장점 | 단점/주의 |
|---|---|
| **지연 최소화**(RTT 제거), Redis **부하·네트워크 대폭 감소** | **일관성**: Redis 값이 바뀌어도 로컬은 옛 값 → **무효화(invalidation)** 필요 |
| Hot key 의 **서버 집중 완화**(5장 hot key 대응) | **메모리**: 앱 인스턴스마다 캐시 복제(인스턴스 수 × 크기) |
| Redis 장애 시 **일부 읽기 계속 가능** | **무효화 전파 지연/유실** 시 stale 데이터 |

### 3-2. 일관성을 맞추는 방법 비교

| 방식 | 동작 | 일관성 | 비고 |
|---|---|---|---|
| **TTL 만 사용** | 로컬 캐시에 **짧은 TTL**(예: 1~30초) | **TTL 만큼 stale 허용** | 가장 단순, 대부분의 경우 충분 |
| **Pub/Sub 으로 무효화** | 쓰기 시 `PUBLISH invalidate key` → 구독 앱이 로컬 삭제 | 구독 유실(순간 단절) 시 stale | 직접 구현, Pub/Sub 의 at-most-once 한계(6장) |
| **Redis 서버 지원 Tracking (6.0+)** | 서버가 **"이 클라이언트가 읽은 키"를 기억**하고 변경 시 **invalidate 푸시** | 푸시 수신 중이면 강한 편 | **3-3 ~ 3-6** |

> 이 책이 **"L1 메모리 캐시" 개념 중심**이라면 위 3-1~3-2(로컬 캐시 + TTL/무효화)를 이해하는 정도로 충분하다. 아래 **서버 지원 Tracking** 은 Redis 6.0+ 의 공식 구현으로, 개념과 동작을 ✅ 직접 확인한 내용을 정리했다.

### 3-3. 서버 지원 클라이언트 사이드 캐싱 — `CLIENT TRACKING`

```
CLIENT TRACKING on [REDIRECT id] [PREFIX p ...] [BCAST] [OPTIN] [OPTOUT] [NOLOOP]
```

**흐름 (기본 모드)**
```
 1) 클라이언트 A:  CLIENT TRACKING on            ← 추적 활성화 (RESP3 권장)
 2) 클라이언트 A:  GET a                          ← 서버가 "A 가 a 를 읽음" 을 Tracking Table 에 기록
                    A 는 응답을 로컬 캐시에 저장
 3) 클라이언트 B:  SET a 1
 4) 서버 → A:     >2 invalidate [a]               ← Push 메시지 (RESP3)
 5) A:            로컬 캐시에서 a 삭제 → 다음 GET 은 서버로
```

✅ 검증 (RESP3, `HELLO 3`):
| 단계 | 결과 |
|---|---|
| `CLIENT TRACKING on` → `GET a`(없음) → 다른 클라이언트가 `SET a 1` | `>2 $10 invalidate *1 $1 a` (**push 수신**) |
| **다시 읽지 않고** 또 `SET a 2` | **푸시 없음** — 무효화 후에는 **추적 항목이 삭제**되므로 **다시 읽어야** 다시 추적됨(한 번 알림 = 1회) |
| `GET a` 재실행 후 `SET a 3` | 다시 invalidate 수신 |
| `FLUSHALL` | `>2 invalidate _` (**null → "전부 무효화"**) |
| **키 만료(expire)** | 만료된 키 접근/삭제 시 invalidate 수신 (키 변경·삭제·만료·**이빅션** 모두 무효화 사유) |
| `CLIENT TRACKINGINFO` | `flags: on`, `redirect: 0`, `prefixes: []` |

### 3-4. 모드별 동작 ✅ 검증

| 모드 | 설명 | 검증 결과 |
|---|---|---|
| **기본 (default)** | **내가 읽은 키**만 추적. 서버 메모리에 **키 → 클라이언트 ID 테이블(Tracking Table)** 유지 | 3-3 |
| **`NOLOOP`** | **내가 쓴 변경은 나에게 알리지 않음**(내 쓰기는 내가 이미 알고 있으니) | `SET k` 후 자기 자신에게 push **없음** |
| **`OPTIN`** | **`CLIENT CACHING yes` 를 직전에 보낸 읽기만** 추적(캐싱할 키만 선택) | `GET p`(CACHING 없이) → 이후 `SET p` 푸시 **없음**. `CLIENT CACHING yes` 후 `GET q` → `SET q` 푸시 **수신** |
| **`OPTOUT`** | 기본은 모두 추적, **`CLIENT CACHING no`** 다음 읽기만 제외 | (문서 기준, 미실행) |
| **`BCAST`** + `PREFIX user:` | **Tracking Table 을 쓰지 않고**, **prefix 에 매칭되는 모든 키의 변경을 알림**(읽었는지와 무관) | `SET user:1`(읽은 적 없음) → **invalidate `user:1` 수신**, `SET other:1` → **없음** |
| **`REDIRECT id`** | 무효화 메시지를 **다른 연결(id)로 전달** (RESP2 용) | RESP2 연결이 `SUBSCRIBE __redis__:invalidate` 로 **`message __redis__:invalidate [rk]`** 수신 |

### 3-5. 모드 선택 트레이드오프

| 모드 | 서버 메모리 | 알림 양 | 적합한 경우 |
|---|---|---|---|
| **기본** | **Tracking Table**(키 × 클라이언트) 사용 → **키·클라이언트 수에 비례** | **읽은 키만** (정확) | 읽는 키 종류가 제한적, 클라이언트 수가 적음 |
| **BCAST** | **거의 0**(prefix 만 기록) | **prefix 매칭 변경 전부**(불필요 알림 많음) | 클라이언트가 많고 키 공간이 prefix 로 구분될 때 |
| **OPTIN** | 캐싱 지정 키만 | 지정 키만 | **캐시할 키가 명확**할 때(대부분 키는 캐시 안 함) |

- **`tracking-table-max-keys`**(기본 **1,000,000** ✅ 확인): 추적 테이블의 **최대 키 수**. 초과 시 **오래된 항목을 무효화하며 제거**(해당 클라이언트에 invalidate 전송) → **서버 메모리 보호**
- `INFO stats`: **`tracking_total_keys`**, `tracking_total_items`, `tracking_total_prefixes` ✅ 확인(마지막 상태에서 prefixes 1)

### 3-6. 구현 시 반드시 고려할 점

| 항목 | 설명 |
|---|---|
| **RESP3 vs RESP2** | RESP3(`HELLO 3`)는 **같은 연결에서 push** 수신. **RESP2 는 같은 연결에서 푸시 불가** → `REDIRECT` 로 **별도 구독 연결** 필요 (RESP2 에서 `REDIRECT` 없이 `TRACKING on` 은 OK 로 응답하지만 ✅ 같은 연결로 invalidate 를 받을 수 없어 의미 없음) |
| **Race condition** | `GET` 응답이 오기 전에 invalidate 가 먼저 도착할 수 있음 → 클라이언트 구현에서 **"읽는 도중 무효화됨" 플래그**로 방어(공식 문서 권장 패턴: 읽기 전에 캐시에 placeholder 를 넣고, invalidate 수신 시 제거) |
| **연결 끊김** | 연결이 끊기면 **무효화를 놓침** → 재연결 시 **로컬 캐시를 전부 비워야** 함 |
| **서버 메모리** | 기본 모드는 **키·클라이언트 수만큼 메모리** 사용 → `tracking-table-max-keys` 로 상한 |
| **TTL 병행** | 무효화 유실에 대비해 **로컬 캐시에도 TTL** 을 둔다(안전망) |
| **캐시 대상** | **읽기 많고 쓰기 적은** 데이터(설정, 상품 정보, 코드 테이블). 쓰기가 잦으면 invalidate 폭주 |
| **클러스터** | 무효화는 **각 노드가 자기 슬롯 키**만 알림 → 클라이언트가 **모든 노드에 TRACKING** 설정해야 함(문서 기준) |
| **라이브러리 지원** | Lettuce(`ClientSideCaching`), Jedis(`ClientSideCache`, 5.x), redis-py 등이 **Tracking 을 래핑**. 직접 구현보다 라이브러리 사용 권장 (지원 여부/버전은 각 문서 확인) |

---

## 4부. 클라이언트 관리 설정 한눈에 보기

### 설정 (✅ 기본값은 7.2.6 `config get` 확인)

| 설정 | 기본값 | 의미 |
|---|---|---|
| `maxclients` | `10000` | 동시 연결 상한 (`ulimit -n` 영향) |
| `timeout` | `0` | 유휴 연결 종료(초), 0=무제한 |
| `tcp-keepalive` | `300` | TCP keepalive 주기(초) |
| `client-query-buffer-limit` | `1gb` | 요청 버퍼 상한 |
| `proto-max-bulk-len` | `512mb` | 단일 인자 최대 크기 |
| `client-output-buffer-limit` | `normal 0 0 0` / `slave 256mb 64mb 60` / `pubsub 32mb 8mb 60` | 클래스별 출력 버퍼 hard/soft |
| `maxmemory-clients` | `0`(비활성) | 클라이언트 총 메모리 한도(이빅션) |
| `tracking-table-max-keys` | `1000000` | Tracking Table 최대 키 수 |
| `io-threads` | `1` | I/O 스레드(1장) — 클라이언트 읽기/쓰기 병렬화 |

### 명령어

| 명령 | 용도 |
|---|---|
| `CLIENT LIST [ID ...] [TYPE normal|master|replica|pubsub]` | 연결 목록과 `qbuf`/`omem`/`tot-mem`/`flags`/`cmd`/`idle` 조회 |
| `CLIENT INFO` / `CLIENT ID` / `CLIENT SETNAME` / `CLIENT SETINFO` | 내 연결 정보/식별 |
| `CLIENT KILL [ID|ADDR|LADDR|USER|MAXAGE|SKIPME]` | 연결 종료 |
| `CLIENT NO-EVICT on|off` | 클라이언트 이빅션 제외 ✅ |
| `CLIENT PAUSE/UNPAUSE [WRITE|ALL]` | 쓰기(또는 전체) 일시 정지 (failover 중 사용) |
| `CLIENT TRACKING / CACHING / TRACKINGINFO / GETREDIR` | 클라이언트 사이드 캐싱 ✅ |
| `INFO clients` / `INFO memory` / `INFO stats` | `connected_clients`, `blocked_clients`, `mem_clients_normal`, `evicted_clients`, `rejected_connections` |
| `redis-cli --pipe` | 대량 데이터 파이프 입력 ✅ |
| `redis-benchmark -P n -c n` | 파이프라인/동시성 벤치마크 ✅ |

---

### ⚠️ 확인하지 못한 부분 (정직 기록)

**로컬에서 직접 확인한 것**: `CLIENT LIST` 버퍼 필드와 유휴 연결 메모리, `client-output-buffer-limit` 기본값과 `normal`/`pubsub` hard limit 초과 시 연결 종료·로그, `client-query-buffer-limit` 초과 종료, `proto-max-bulk-len` 에러, `maxclients` 기동 옵션 동작(`rejected_connections`), `maxmemory-clients` 이빅션과 `CLIENT NO-EVICT`, 파이프라인 처리량(`redis-benchmark -P`, 직접 소켓 측정, `redis-cli --pipe`), `CLIENT TRACKING` 기본/`NOLOOP`/`OPTIN`/`BCAST`+`PREFIX`/`REDIRECT`, FLUSHALL·만료 무효화, 무효화 1회성, `TRACKINGINFO`.

**문서/지식 기준이며 재현하지 않은 것**:
- **soft limit(soft 초 유지 후 종료)**: hard limit 만 재현. soft 동작은 문서 기준
- **replica class 의 sync 루프, 7.0 shared replication buffer, `mem_not_counted_for_evict`** 세부
- **`OPTOUT` 모드, 클러스터에서 Tracking**, race condition 방어 패턴 구현, **연결 끊김 시 캐시 전체 비움** 요구사항
- **`maxclients` 와 `ulimit -n` 의 관계**(기동 시 자동 하향) 수치
- **런타임 `CONFIG SET maxclients 3` 이 기존/신규 연결에 즉시 적용되지 않는 현상** — 재현했으나 원인 미확인(기동 옵션은 정상 동작)
- **클라이언트 라이브러리**(Lettuce/Jedis/Spring/redis-py)의 파이프라이닝·클라이언트 캐싱 API 와 기본 동작은 문서 기준이며 실행해보지 않음
- **성능 수치는 loopback 단일 호스트**(`io-threads 1`, 단일 클라이언트) 결과이며, 네트워크 RTT·멀티 클라이언트·실제 데이터 크기에서는 달라진다
- **3-1~3-2 의 로컬 캐시(Caffeine 등)와 Pub/Sub 무효화 패턴**은 개념 설명이며 코드를 실행하지 않음. 책에서 다루는 구체 구현(예: Spring Cache + Caffeine)은 **책 내용 확인 필요**
