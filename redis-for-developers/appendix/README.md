## 부록. 책을 읽으며 추가로 궁금했던 내용 정리

> 책 본문에는 없거나 약하게 다뤄진 부분 중 **직접 궁금했던 내용**을 **로컬 Redis 7.2.6 (macOS)** 에서 실행·관찰해 정리했다. 확인한 내용은 `✅ 검증`, 소스/문서 지식 기준은 `(문서 기준)` 으로 표시했다.

#### 목차

- [Q1. Redis 의 스레드 구조 — "메인 스레드 1개 + replica 3개"?](#q1-redis-의-스레드-구조--메인-스레드-1개--replica-3개)
- [Q2. TTL 이 지난 키는 내부적으로 어떻게 관리·삭제되나](#q2-ttl-이-지난-키는-내부적으로-어떻게-관리삭제되나)
- [Q3. 키를 지웠는데 RSS 가 안 줄어드는 이유 — 메모리 해제와 allocator](#q3-키를-지웠는데-rss-가-안-줄어드는-이유--메모리-해제와-allocator)
- [Q4. dict 점진적 리해싱 — 키가 수백만 개여도 멈추지 않는 이유](#q4-dict-점진적-리해싱--키가-수백만-개여도-멈추지-않는-이유)
- [Q5. 자료구조 인코딩 전환 — listpack/hashtable/quicklist/skiplist](#q5-자료구조-인코딩-전환--listpackhashtablequicklistskiplist)
- [Q6. maxmemory eviction 내부 — 근사 LRU/LFU](#q6-maxmemory-eviction-내부--근사-lrulfu)
- [Q7. MULTI/EXEC 와 WATCH 내부](#q7-multiexec-와-watch-내부)
- [Q8. 블로킹 명령(BLPOP, WAIT)은 어떻게 서버를 멈추지 않고 대기하나](#q8-블로킹-명령blpop-wait은-어떻게-서버를-멈추지-않고-대기하나)
- [Q9. 복제 내부 — replid, offset, backlog, PSYNC](#q9-복제-내부--replid-offset-backlog-psync)
- [Q10. Stream 내부 — radix tree + listpack, Consumer Group PEL](#q10-stream-내부--radix-tree--listpack-consumer-group-pel)
- [Q11. RESP 프로토콜과 reply 경로](#q11-resp-프로토콜과-reply-경로)
- [Q12. Lua/Function 실행 모델](#q12-luafunction-실행-모델)
- [Q13. RDB 파일 포맷 해부](#q13-rdb-파일-포맷-해부)
- [Q14. 클러스터 gossip 메시지 — 얼마나, 어떻게 오가나](#q14-클러스터-gossip-메시지--얼마나-어떻게-오가나)

---

## Q1. Redis 의 스레드 구조 — "메인 스레드 1개 + replica 3개"?

### 1-1. 결론 먼저

**"replica 3개"가 아니라 "백그라운드(bio) 스레드 3개"** 가 맞다. (1장에서 정리한 "메인 스레드 1 + 별도 스레드 3"과 같은 내용을 기억 과정에서 *replica* 로 섞어 기억한 것으로 보인다.)

- **replica(복제본)** 는 **스레드가 아니라 별도의 Redis 서버 프로세스**(보통 다른 호스트)다. 8장의 복제와는 다른 층위의 개념.
- **Redis 프로세스 내부** 에는 **명령을 실행하는 메인 스레드 1개 + 백그라운드 작업용 bio 스레드 3개** 가 있다.

✅ 검증 — 실행 중인 서버의 스레드 수 (`ps -M -p <pid>`):

| 서버 | 스레드 수 | 구성 |
|---|---|---|
| 기본 설정 (`io-threads 1`) | **4** | 메인 1 + bio 3 |
| `--io-threads 4 --io-threads-do-reads yes` | **7** | 메인 1 + bio 3 + **I/O 스레드 3** (메인이 I/O 스레드 4개 중 1개를 겸함) |

### 1-2. 프로세스/스레드 전체 그림

```
┌────────────────────── redis-server 프로세스 (PID 1234) ──────────────────────┐
│                                                                               │
│  ★ 메인 스레드 (이벤트 루프)                                                   │
│     · 클라이언트 소켓 accept / 읽기 / 파싱                                     │
│     · ★ 명령 실행 (데이터 접근은 오직 여기서만 → 락 불필요, 원자성 보장)        │
│     · 응답 버퍼 작성 / 쓰기, AOF 버퍼 기록, 복제 스트림 전송                   │
│     · serverCron(hz): active expire, eviction, 통계, 복제/클러스터 관리        │
│                                                                               │
│  ┌── bio 스레드 ×3 (Background I/O, 느린 syscall 을 메인에서 분리) ──┐         │
│  │  ① close_file  : close(fd) — 큰 파일 close 가 느릴 수 있어 분리   │         │
│  │  ② aof_fsync   : appendfsync everysec 의 fsync() 호출             │         │
│  │  ③ lazy_free   : UNLINK / FLUSHALL ASYNC / lazyfree-lazy-* 해제   │         │
│  └───────────────────────────────────────────────────────────────────┘         │
│  (선택) I/O 스레드 N-1 개: 소켓 read/write 만 병렬 (io-threads ≥ 2)            │
└───────────────────────────────────────────────────────────────────────────────┘
        │ fork()                                      │ 네트워크(복제)
        ▼                                             ▼
┌────────────────────────┐                   ┌──────────────────────────┐
│ 자식 프로세스            │                   │ replica = 별도 서버 프로세스 │
│ redis-rdb-bgsave /      │                   │ (자기 메인 스레드·bio 를 따로 가짐)│
│ redis-aof-rewrite       │                   └──────────────────────────┘
│ (스레드 아님, 프로세스)   │
└────────────────────────┘
```

| 구분 | 개수 | 하는 일 | 확인 |
|---|---|---|---|
| **메인 스레드** | 1 | 이벤트 루프 + **모든 명령 실행** + cron | 4개 스레드 중 1 ✅ |
| **bio 스레드** | 3 (7.2 기준) | close / AOF fsync / lazy free | 스레드 수 4 ✅, `lazyfreed_objects` 증가 ✅ (아래) |
| **I/O 스레드** | 0 (기본) ~ N-1 | 소켓 read/write 만 (명령 실행은 안 함) | `io-threads 4` 에서 7개 ✅ |
| **fork 자식 프로세스** | BGSAVE/rewrite 중에만 | RDB 저장, AOF rewrite | `redis-rdb-bgsave` 프로세스 ✅ |
| **replica** | 별개 서버 | 복제본 | 스레드 아님 |

> 스레드 이름(`bio_close_file`, `bio_aof`, `bio_lazy_free`, `io_thd_N`)은 리눅스 `top -H`/`ps -T` 에서 확인되는 이름(문서 기준). 이 환경(macOS)에서는 스레드 이름을 보지 못해 **개수만** 확인했다. bio 스레드 종류/개수는 **버전에 따라 달라질 수 있다**(예: 7.x 후반/8.x 에서 증가 여부는 확인하지 못함).

### 1-3. 메인 스레드: 이벤트 루프 한 사이클

Redis 는 **I/O 멀티플렉싱(Linux `epoll`, macOS `kqueue`)** 으로 **한 스레드가 수천 개 연결을 번갈아 처리**한다 (1장).

```
while (!stop) {                          // aeMain
    beforeSleep();                       // ① 다음 poll 전에 할 일
    aeProcessEvents();                   // ② 이벤트 대기·처리
}

① beforeSleep (문서 기준 주요 항목)
   · active expire "fast cycle"          — 만료 키 빠른 정리 (Q2)
   · 블록된 클라이언트(BLPOP 등) 깨우기
   · AOF 버퍼를 파일에 write (+ fsync 정책 처리)
   · 클라이언트 output buffer 를 소켓으로 write (handleClientsWithPendingWrites)

② aeProcessEvents
   · epoll_wait/kqueue 로 "읽을 수 있는 소켓" 대기
   · File event: 소켓 readable → readQueryFromClient → 명령 파싱·실행
   · Time event: serverCron (hz 주기, 기본 10회/초)
```

### 1-4. 요청 한 건의 처리 flow (SET key value)

```
클라이언트              메인 스레드                                       bio / 자식 / replica
   │ ──SET k v──▶ ① 소켓 readable 이벤트
   │               ② readQueryFromClient: query buffer 에 읽기
   │               ③ processInputBuffer: RESP 파싱 → argv
   │               ④ processCommand: 명령 조회, 인증/ACL, 인자 수, maxmemory(eviction/OOM),
   │                  MULTI 큐잉, 레플리카 읽기전용 등 검사
   │               ⑤ call(): 실제 실행 (dict 에 SET) ◀── 여기서만 데이터 변경
   │               ⑥ propagate: AOF 버퍼에 append / replica 용 복제 스트림·backlog 에 기록
   │               ⑦ addReply: 응답을 client output buffer 에 기록 (+ "쓰기 대기" 큐에 등록)
   │               ⑧ beforeSleep: output buffer → 소켓 write,
   │                  AOF 버퍼 → 파일 write                          ─▶ (everysec) aof_fsync bio 가 fsync
   │ ◀─── +OK ──── ⑨ 응답 전송                                        ─▶ replica 연결 클라이언트에도 write
```

- **④~⑤ 구간이 직렬**이므로 **한 명령이 오래 걸리면 뒤 모든 명령이 대기**한다 ✅ (13장 측정: `KEYS *` 204ms 동안 다른 `PING` 최대 90ms 지연, Lua 무한 루프는 `BUSY` 응답)
- `io-threads ≥ 2` 이면 **② 읽기와 ⑧⑨ 쓰기** 를 I/O 스레드가 분담할 수 있다. **③~⑥ 파싱 이후 실행은 여전히 메인 스레드** — 그래서 I/O 스레드는 **네트워크 병목이 큰 워크로드**(작은 값, 높은 QPS, 많은 연결)에서만 이득이다.

### 1-5. bio 스레드 3개 상세

| 스레드 | 담당 | 왜 분리? | 연관 설정/지표 |
|---|---|---|---|
| **close_file** | AOF/RDB 임시 파일 등의 `close()` | 큰 파일 close 시 디스크 flush 로 느릴 수 있음 | — |
| **aof_fsync** | `appendfsync everysec` 일 때 **1초마다 `fsync()`** | fsync 는 디스크 상태에 따라 수~수백 ms 블로킹 → 메인이 막히면 안 됨 | `aof_pending_bio_fsync`, `aof_delayed_fsync` ✅ 필드 확인 |
| **lazy_free** | **`UNLINK`, `FLUSHALL/FLUSHDB ASYNC`, `lazyfree-lazy-*`** 로 지정된 큰 객체 메모리 해제 | 큰 키 해제(free)는 오래 걸림 | `lazyfree_pending_objects`, `lazyfreed_objects` ✅ |

✅ 검증 — lazy_free 스레드가 실제로 일함:
```
60만 필드 해시 big2 생성
INFO:  lazyfreed_objects:1   →  UNLINK big2  →  lazyfreed_objects:2    (lazyfree_pending_objects:0 로 즉시 처리 완료)
```
13장 측정과 연결: **`DEL`(메인에서 동기 해제) 147ms vs `UNLINK`(lazy_free bio 에 위임) 9ms**.

- **`appendfsync always`** 는 이 bio 를 쓰지 않고 **메인 스레드가 직접 fsync** → 처리량 급락 ✅ (13장: 약 313 req/s vs everysec 약 11,800 req/s)
- **bio 는 "무거운 해제/디스크 syscall" 만** 맡고, **데이터를 읽고 쓰는 로직은 하지 않는다**(= 데이터 구조는 메인만 접근하므로 락이 필요 없음).

### 1-6. fork 자식 프로세스 (스레드가 아님)

`BGSAVE`, `BGREWRITEAOF`, 복제 **full sync** 의 RDB 생성은 **`fork()` 로 만든 자식 프로세스** 가 수행한다. 부모는 계속 명령을 처리하고, **COW(Copy-On-Write)** 로 스냅샷 시점의 메모리를 공유한다 (7장).

✅ 검증: `BGSAVE` 직후 프로세스 목록
```
37685  ppid 1       redis-server *:7400            ← 부모(메인)
37842  ppid 37685   redis-rdb-bgsave *:7400        ← 자식 (부모 PID = 37685)
```
- fork 시점의 **페이지 테이블 복사** 가 메인을 잠깐 멈춘다 → `latest_fork_usec` (✅ 로컬 2.5ms / 121MB, 대용량은 훨씬 길어짐 — 13장)

### 1-7. replica 와의 관계 (8장 연결)

- replica 는 **독립 Redis 서버**: 자기 메인 스레드·bio 를 가진다. 마스터와는 **TCP 연결(복제 스트림)** 로만 연결.
- **마스터 입장**: replica 도 **"클라이언트 연결 하나"** 로 취급(`flags=S`). 마스터 **메인 스레드**가 쓰기 명령을 replica 연결의 output buffer 에 쌓아 보낸다(→ 12장 `client-output-buffer-limit replica`).
- **full sync 의 RDB 생성**은 fork 자식(디스크 방식) 또는 자식이 소켓으로 직접 전송(diskless)(8장).
- **replica 의 메인 스레드**는 마스터 복제 스트림을 읽어 **자기 데이터에 명령을 적용**하고, 클라이언트 읽기도 처리한다.

### 1-8. 왜 싱글 스레드로 빠른가 / 한계

| 이유 | 설명 |
|---|---|
| 메모리 기반 | 대부분 명령이 O(1)~O(log N), 디스크 대기 없음 |
| **락/컨텍스트 스위치 없음** | 데이터 접근이 메인 하나 → 동기화 비용·데드락·경합 없음 |
| I/O 멀티플렉싱 | 스레드 1개로 다수 연결 |
| 효율적 자료구조 | listpack/quicklist/skiplist (3장) |

| 한계 | 대응 |
|---|---|
| **느린 명령이 전체를 막음** ✅ | `KEYS`→`SCAN`, `DEL`→`UNLINK`, big key 분할 (13장) |
| **CPU 코어 1개로 제한** | 읽기는 replica 분산, **클러스터로 샤딩**, I/O 스레드(네트워크 병목 시) |
| fork 지연 | 인스턴스 크기 제한, THP off |

> **Redis 8** 등 최신 버전은 I/O 스레딩 모델이 크게 개선되었다는 정보가 있으나(문서 기준) **이 환경에서 확인하지 않았다.** 위 구조는 **7.2.6 기준**.

---

## Q2. TTL 이 지난 키는 내부적으로 어떻게 관리·삭제되나

### 2-1. 결론 먼저

**둘 다 한다.** 키마다 타이머를 두지 않고, **① 접근할 때 확인(Lazy/Passive)** + **② 주기적으로 샘플링해서 정리(Active)** 를 **함께** 쓴다. (복제 환경에서는 ③ **마스터가 만료시켜 `DEL` 을 replica 에 전파**.)

| 방식 | 언제 | 목적 |
|---|---|---|
| **① Lazy (Passive) 만료** | **키에 접근하는 순간** TTL 확인, 지났으면 삭제 후 "없음" 처리 | **정확성**: 만료된 값을 절대 반환하지 않음 |
| **② Active 만료** | **주기적으로(백그라운드 아님, 메인 스레드 cron)** 만료 키를 샘플링해 삭제 | **메모리 회수**: 아무도 접근하지 않는 만료 키가 쌓이는 것 방지 |
| **③ 복제 전파** | 마스터가 만료 처리하면 **`DEL` 을 replica·AOF 로 전파** | replica 는 **스스로 삭제하지 않음** |

### 2-2. 내부 자료구조 — TTL 은 어디에 저장되나

Redis 는 DB 마다 **두 개의 해시 테이블(dict)** 을 가진다.

```
redisDb {
   dict *dict;      // 키 공간:  key → value
   dict *expires;   // 만료 테이블: key → 만료 시각(절대 시간, unix ms)  ← TTL 이 있는 키만 존재
}
```

- **TTL 이 있는 키만 `expires` 에 들어간다.** 만료 시각은 **남은 시간이 아니라 "절대 시각(밀리초)"** 으로 저장(문서 기준) → 접근 시 `현재 시각 > 만료 시각` 만 비교하면 된다.
- 별도의 **타이머/정렬 구조(min-heap 등)** 는 **없다**. 그래서 "가장 먼저 만료될 키"를 O(1)로 찾을 수 없고, Active 만료가 **샘플링** 방식인 이유가 된다.
- ✅ 관찰: `INFO keyspace` → `db0:keys=300100,expires=100,avg_ttl=0` — **전체 키 수와 TTL 키 수(expires)** 가 따로 집계됨.
- AOF 에는 **절대 시각** 으로 기록된다 ✅ (아래 2-7).

### 2-3. ① Lazy (Passive) 만료 — 접근 시 확인

키를 찾는 내부 함수(`lookupKey…` → `expireIfNeeded`)가 **키를 조회할 때마다** `expires` 를 확인한다. 지났으면 **그 자리에서 삭제**하고 "키 없음"으로 응답한다.

✅ 검증 — **Active 만료를 꺼서(`DEBUG SET-ACTIVE-EXPIRE 0`) Lazy 만 남긴 상태**:

```
SET lk:1..100 v PX 100   (100ms 후 만료)  →  1초 대기, 아무도 접근하지 않음

DBSIZE            → 300100   (만료됐지만 그대로 남아 있음)     ← 키가 실제로 안 지워짐
INFO keyspace     → db0:keys=300100, expires=100
INFO stats        → expired_keys:0

TTL lk:1 → -2   PTTL lk:2 → -2   EXISTS lk:3 → 0   GET lk:4 → (nil)       ← 응답은 "없음"
DBSIZE → 300096    expired_keys:4                                           ← 접근한 4개만 삭제됨
```

**→ "조회하면 없다고 나오는 것"은 접근 시점에 지워 보여주는 Lazy 동작이고, 접근하지 않은 키는 메모리에 남아 있다.** (`DBSIZE` 와 `INFO keyspace` 의 `keys` 에도 **만료됐지만 아직 안 지워진 키가 포함**됨.)

#### 어떤 명령이 Lazy 삭제를 유발하나 ✅ 검증 (5개 키 만료 후 각 명령 1회 실행)

| 명령 | 만료 키 삭제? | 결과 |
|---|---|---|
| `TTL` / `PTTL` / `EXISTS` / `TYPE` / `STRLEN` / `GET` / `MGET` / `TOUCH` / `DUMP` / `OBJECT ENCODING` | **삭제됨**(1개) | `-2` / `0` / `none` / (nil) 등 "없음" 응답 |
| `EXPIRE` / `PERSIST` / `GETEX` | **삭제됨** | 대상 없음(`0`) — **만료된 키에는 TTL 변경 불가** |
| `RENAME` / `COPY` | **삭제됨** | `ERR no such key` / `0` |
| `DEL` / `UNLINK` | 삭제됨(`0` 반환) | 이미 만료 → 삭제 건수 0 |
| **`SET … NX`, `SETNX`, `INCR`, `APPEND`** | **삭제 후 새 키로 처리** | `SET NX → OK`, `INCR → 1`, `APPEND → 1` (만료된 값은 없던 것으로 취급) |
| **`SCAN 0 COUNT 100000`** | **전부 삭제됨**(5개 → 0) | SCAN 이 순회하며 만료 키를 정리 |
| **`RANDOMKEY`** | **삭제됨**(5개 → 0, 후보를 뽑다 만료 키를 정리) | 빈 응답 |
| **`KEYS *`** | **삭제 안 됨**(5개 그대로), 결과에서만 **제외** | 만료 키는 응답에 안 나옴 |
| `DBSIZE` | 삭제 안 됨, **만료 키 포함 개수** | `5` |
| `DEBUG OBJECT` | 삭제 안 됨 (디버그용, 만료 검사 우회) | `Value at:...` |

- 쓰기 명령(`SET NX`, `INCR` 등)은 **만료 키를 삭제한 뒤 이어서 실행** → 새 값에 TTL 은 없음.
- **`SET key value`**(옵션 없이) 는 **기존 TTL 을 제거**한다 ✅ (`SET d v` 후 `SET d v2` → `TTL d = -1`). TTL 유지는 `KEEPTTL`.

### 2-4. ② Active 만료 — 주기적 샘플링

**접근되지 않는 만료 키**가 쌓여 메모리를 차지하는 것을 막기 위한 **주기 작업**이다. **백그라운드 스레드가 아니라 메인 스레드**가 수행한다 (그래서 CPU 시간 제한이 있다).

**동작 (문서 기준)**
```
serverCron 이 hz 주기(기본 10 = 100ms 마다)로 activeExpireCycle(SLOW) 호출
  + beforeSleep 에서 activeExpireCycle(FAST) 도 짧게 호출 (이벤트 루프 사이)

DB 마다:
   do {
       expires 테이블에서 "무작위로 20개" 키 샘플 (ACTIVE_EXPIRE_CYCLE_KEYS_PER_LOOP)
       그중 만료된 키 삭제
   } while (샘플 중 만료 비율 > 10%  &&  시간 제한 이내)     ← 만료 키가 많다고 판단되면 반복

시간 제한: 한 번 cycle 이 CPU 시간의 약 25% 이내 (hz=10 → 약 25ms)
```
- **"전수 검사"가 아니라 확률적 샘플링.** 만료 키 비율이 10% 이하로 내려가면 멈춘다 → **일부 만료 키는 한동안 남을 수 있고**, 그 간극을 **Lazy 가 보완**한다.
- `active-expire-effort`(기본 **1** ✅, 범위 1~10): 값을 올리면 샘플 수·시간 한도를 늘려 **더 공격적으로** 정리(CPU↑, 만료 지연↓).
- `hz`(기본 **10** ✅, `dynamic-hz yes` ✅): 올리면 cron 이 더 자주 돌아 만료 정리가 빨라지지만 **idle CPU↑**.

✅ 검증 — **Active ON, 20만 개 키를 같은 시각(1초 TTL)에 만료**시켰을 때:

| 설정 | 20만 키가 전부 사라질 때까지 (삽입 완료 후) | 비고 |
|---|---|---|
| `hz 10`(기본) | **약 3.7초** | TTL 1초 + 정리에 약 2.7초 추가 |
| `hz 100` | **약 2.6초** | cron 이 더 자주 → 약 1.1초 단축 |

```
INFO stats (위 2회 실험 누적 40만 키 만료 후)
 expired_keys:400005
 expired_stale_perc:54.46                   ← 직전 사이클 샘플 중 "만료됐는데 아직 남은" 키 비율 추정
 expired_time_cap_reached_count:315         ← 시간 제한(25%)에 걸려 사이클을 중단한 횟수
 expire_cycle_cpu_milliseconds:1147         ← active expire 에 쓴 누적 CPU 시간 (약 1.1초)
```
→ **같은 시각에 대량 만료**되면 Active 가 **CPU 시간 제한에 걸려 여러 사이클에 나눠** 처리하고, 그 동안 **만료된 키가 메모리에 남는다**(`expired_stale_perc` 상승). 5장 "avalanche" 대응(**TTL 에 랜덤 지터**)의 근거다.

### 2-5. 만료가 "실행"될 때 일어나는 일

키가 만료돼 삭제되는 순간(Lazy/Active 공통):

```
1) 키 삭제 (기본 동기 DEL, lazyfree-lazy-expire yes 면 UNLINK 처럼 bio 로 해제)   ← 기본 no ✅
2) 삭제를 "DEL key" 로 AOF 와 replica 에 전파                                      ← ✅ AOF 에 DEL b 확인
3) 키스페이스 이벤트 'expired' 발행 (notify-keyspace-events 에 x 포함 시)
4) expired_keys 통계 증가
```

✅ 검증 — **`expired` 이벤트는 TTL 이 도달한 시점이 아니라 "실제로 삭제된 시점"에 발행됨** (`notify-keyspace-events Ex`, TTL 500ms):

| 상황 | 이벤트 수신 |
|---|---|
| Active ON | **0.54초 후**(TTL 0.5초 직후) 수신 |
| Active OFF, 3초간 접근 없음 | **수신 안 됨** (아직 삭제되지 않았으므로) |
| 그 후 `GET` 으로 접근 | **그때서야 수신** (TTL 로부터 3.0초 후) |

→ **"만료 알림에 의존하는 로직(예: 만료 시 주문 취소)" 은 정확한 시각을 보장하지 않는다.** 정확한 시각이 필요하면 Sorted Set(score=만료 시각) + 폴링, 지연 큐 등으로 구현. (Pub/Sub 이벤트이므로 구독자 단절 시 유실, 6장)

### 2-6. ③ replica 의 만료 처리 (복제 환경)

**replica 는 스스로 키를 삭제하지 않는다.** 마스터가 만료시켜 **`DEL`** 을 보내주길 기다린다 (키 불일치·일관성 보장). 대신 **읽기에서는 논리적으로 만료된 것처럼 응답**한다.

✅ 검증 — 마스터 Active 만료를 끄고 TTL 300ms 키를 만든 뒤 TTL 경과(마스터 접근 없음):

| 시점 | 마스터 | replica |
|---|---|---|
| TTL 이전 | `GET rk → v` | `GET rk → v` |
| **TTL 경과 후, 마스터 미접근** | `DBSIZE 1` (아직 남음) | **`GET rk → (nil)`**, **`TTL rk → -2`**, `EXISTS rk → 0` 이지만 **`DBSIZE 1`**, `expired_keys:0` |
| **마스터에서 `GET rk`** (Lazy 삭제 → `DEL` 전파) | `DBSIZE 0` | **`DBSIZE 0`** (DEL 수신 후 삭제) |

- 즉 replica 는 **"응답은 만료 처리, 실제 삭제는 마스터의 DEL 을 기다림"**. 그래서 **마스터에서 만료가 지연되면 replica 메모리에도 만료 키가 남는다.**
- **마스터 장애 → replica 승격** 시: 승격된 노드가 이후부터 직접 만료를 처리 (Active/Lazy 시작). 승격 직후 **만료 키가 한꺼번에 남아 있어** CPU 스파이크가 날 수 있음(문서 기준).

### 2-7. 영속성과 TTL (7장 연결)

✅ **AOF**: TTL 은 **절대 시각**으로 기록되고, 만료 삭제는 `DEL` 로 기록된다. (`appendfsync always`, 7.2.6 incr AOF)
```
SET a v EX 100   →  SET a v PXAT 1792330629895        (상대 시간 100초 → 절대 ms 시각으로 변환)
SET b v PX 100   →  SET b v PXAT 1792330530006
SET c v ; EXPIRE c 500  →  set c v ; PEXPIREAT c 1792331029925
(b 가 만료돼 접근됨)   →  DEL b
```
- 절대 시각으로 기록하므로 **재시작/AOF 재생 시에도 "원래 만료 시각"이 유지**된다(재생해도 TTL 이 다시 길어지지 않음).
- **RDB**: 만료 시각(절대 시간)을 함께 저장. **로드 시 마스터는 이미 만료된 키를 건너뛰고**, replica 는 로드해 두고 마스터 `DEL` 을 기다린다(문서 기준, 로컬 미확인).
- 서버 **시계가 틀어지거나 점프**하면(NTP) 만료 시각 판정이 달라질 수 있음 — 서버 시간 동기화 필요.

### 2-8. TTL 관련 명령 동작 요약 ✅

| 명령 | 동작 |
|---|---|
| `EXPIRE/PEXPIRE key n`, `EXPIREAT`, `PEXPIREAT` | TTL 설정(초/밀리초, 상대/절대). 이미 지난 시각이면 **즉시 삭제** (문서 기준) |
| `TTL`/`PTTL` | 남은 시간. **`-1` = 키는 있으나 TTL 없음**, **`-2` = 키 없음(만료 포함)** ✅ |
| `PERSIST` | TTL 제거 |
| `SET k v EX/PX/EXAT/PXAT`, `KEEPTTL`, `GETEX` | TTL 과 함께 설정 / 유지 / 읽으며 변경 |
| `SET k v`(옵션 없음) | **TTL 제거** ✅ |
| `EXPIRE k n NX|XX|GT|LT` (7.0+) | 조건부 TTL 변경 |

### 2-9. 정리 — 메모리/성능 관점의 시사점

| 포인트 | 설명 | 대응 |
|---|---|---|
| **만료 ≠ 즉시 메모리 해제** | Lazy 로 접근되거나 Active 가 샘플링할 때까지 메모리 유지 ✅ (`DBSIZE` 가 만료 키 포함) | `INFO` 의 `expired_stale_perc` 모니터링, 필요 시 `active-expire-effort`/`hz` 상향 |
| **동일 시각 대량 만료** | Active 가 CPU 상한(약 25%)에 걸려 지연 + 메인 스레드 점유 ✅ (`expired_time_cap_reached_count`, `expire_cycle_cpu_milliseconds`) | **TTL 지터**, 배치 분산 |
| **big key 만료** | 만료 삭제가 **동기 해제**(기본 `lazyfree-lazy-expire no` ✅) → 메인 블로킹 | `lazyfree-lazy-expire yes` |
| **`DBSIZE`/`keys` 해석** | 만료 키가 포함될 수 있음. `KEYS` 는 응답에서만 제외 ✅ | 정확한 "유효 키 수"는 의미 없음, 대략치로 해석 |
| **replica 메모리** | 마스터 DEL 이 와야 정리 → 마스터와 비슷한 만료 지연 | 마스터 만료 지연 해소가 곧 replica 해소 |
| **만료 이벤트 의존** | 삭제 시점에 발행 ✅ | 정확한 시각 필요 시 별도 구조 |
| **읽기 많은 키의 만료** | Lazy 로 즉시 정리 → 문제 적음 | — |
| **아무도 안 읽는 키의 만료** | Active 에 전적으로 의존 → 샘플링 확률 | TTL 있는 키 수·분포 점검 |

---

## Q3. 키를 지웠는데 RSS 가 안 줄어드는 이유 — 메모리 해제와 allocator

### 3-1. 결론 먼저

Redis 는 객체를 지우면 **allocator(`malloc`/`jemalloc`)에게 메모리를 반납**한다. 하지만 allocator 는 **OS 에 곧바로 돌려주지 않고 자기 풀에 보관**하는 경우가 많다. 그래서 **`used_memory`(Redis 가 할당했다고 보는 양)는 줄어도 `used_memory_rss`(OS 가 보는 실제 점유량)는 그대로**일 수 있다. 이 차이가 `mem_fragmentation_ratio` (= RSS / used_memory) 다.

```
mem_fragmentation_ratio = used_memory_rss / used_memory
  ≈ 1.0~1.5  정상          > 1.5  단편화(RSS 낭비)          < 1.0  스왑으로 밀려남(위험)
```

### 3-2. 실험 ✅ 검증 (100B 값 × 100만 키 → 90% 삭제)

> ⚠️ 이 빌드는 **`malloc=libc`** 이다(`redis-server -v` 의 `malloc=libc`, `INFO memory` 의 `mem_allocator:libc`). 리눅스 기본 빌드가 쓰는 **jemalloc 과 반환·단편화 특성이 다르다**. 아래는 **"allocator 가 메모리를 쥐고 있는" 현상 자체**를 보여주는 예이며, 정확한 수치는 jemalloc 에서 다를 수 있다.

| 단계 | used_memory | used_memory_rss | fragmentation ratio |
|---|---|---|---|
| 빈 서버 | 1.1 MB | 3.6 MB | 3.29 (작아서 의미 없음) |
| 키 100만 개 삽입 | **201.6 MB** | **216.1 MB** | **1.07** |
| **90% 삭제**(10개 중 9개 `DEL`) | **29.9 MB** | **216.3 MB** (그대로) | **7.51** |
| `MEMORY PURGE` 후 | 21.5 MB | 217.4 MB | 10.11 |
| `FLUSHALL` 후 | 1.3 MB | 26.8 MB | 20.68 |

- **키 90%를 지웠는데 RSS 는 216MB 그대로** — 살아있는 키가 곳곳(페이지 전체에 흩어져)에 남아 **페이지를 OS 에 반환하지 못한다**. 이것이 **내부 단편화**.
- **`FLUSHALL`(전부 삭제)** 에서야 RSS 가 크게 줄었다(26.8MB). 다만 **완전히 돌려주진 않는다**.
- `MEMORY PURGE` 는 `OK` 를 반환했지만 **libc 환경에서는 효과 없음**(jemalloc 의 dirty page purge 용 명령).
- `MEMORY MALLOC-STATS` → `Stats not supported for the current allocator` (libc).
- **`activedefrag yes` 시도 → `ERR ... Active defragmentation cannot be enabled: it requires a Redis server compiled with a modified Jemalloc`** ✅ — **액티브 디프래그는 jemalloc 빌드에서만** 가능.

### 3-3. 어떻게 해석하고 대응하나

| 관찰 | 의미 | 대응 |
|---|---|---|
| `used_memory` ↓, RSS 그대로, ratio ↑ | **단편화** — 키를 많이 지운 뒤 흔함 | ① **그냥 두면 재사용**: 이후 새 키가 같은 빈 공간을 채움(RSS 는 더 안 커짐) ② `activedefrag yes`(jemalloc) ③ **재시작/failover 로 메모리 재구성**(replica 를 승격) |
| ratio < 1 | **스왑** 사용 중(RSS 가 실제보다 작게 보임) | 즉시 조치: 메모리 증설/스왑 금지(13장) |
| RSS 만 크고 `used_memory` 작음 | fork COW, 버퍼 외 영역 포함 가능 | `INFO memory` 의 `mem_clients_*`, `mem_aof_buffer`, `used_memory_overhead` 확인 |
| `maxmemory` 는 **`used_memory`** 기준 | RSS 가 커도 eviction 은 `used_memory` 로 판단 | **호스트 메모리는 RSS 기준으로 계획**(`maxmemory` 를 호스트의 60~75% 로, 13장) |

**jemalloc 에서 (문서 기준)**: 크기 클래스별 arena/bin 에 메모리를 보관하고 `allocator_frag_ratio`, `allocator_active`, `allocator_resident` 로 **allocator 레벨 단편화**를 따로 보여준다. `INFO memory` 의 `allocator_frag_ratio`(allocator 내부) 와 `rss_overhead_ratio`(RSS/allocator_resident) 를 분리해 보면 원인(allocator vs OS)을 구분할 수 있다. **Redis 7.0 이후 `MEMORY PURGE`/백그라운드 purge 는 jemalloc 에서만 유효**.

---

## Q4. dict 점진적 리해싱 — 키가 수백만 개여도 멈추지 않는 이유

### 4-1. 문제

해시 테이블은 **요소 수 ≈ 버킷 수** 가 되면 충돌이 늘어 **테이블을 2배로 키워야** 한다. 일반 구현은 **모든 키를 새 테이블에 한 번에 재배치(rehash)** 하는데, 키가 100만 개면 **그 순간 메인 스레드가 멈춘다**. Redis 는 이를 **점진적 리해싱(incremental rehashing)** 으로 피한다.

### 4-2. 구조 — 테이블 2개

```
dict { ht[0]: 기존 테이블, ht[1]: 확장 목표 테이블, rehashidx: 현재 옮기는 버킷 번호(-1 이면 rehash 안 함) }
```

- **확장 시작**: 요소 수 ≥ 버킷 수(비율 1:1)이면 `ht[1]` 을 **2배 크기**로 만들고 `rehashidx = 0` 으로 설정
- **요청 처리 중 조금씩 이전**: **키 조회/삽입/삭제(dict 연산)가 일어날 때마다 `ht[0]` 의 버킷 1개(들)를 `ht[1]` 로 옮김**
- **`serverCron`(activerehashing yes, 기본) 이 100ms 마다 1ms 동안 추가 이전** — 요청이 없어도 진행
- 이전 중에는 **조회 시 `ht[0]` → `ht[1]` 순으로 둘 다 확인**, **삽입은 `ht[1]` 에만**
- 모두 옮기면 `ht[0] = ht[1]`, `ht[1]` 비움

### 4-3. 실험 ✅ 검증 (`DEBUG HTSTATS 0`)

```
키 1,048,576 개(= 2^20) 입력 후:   table size: 1048576   number of elements: 1048576      (rehash 없음)

activerehashing no 로 끄고 키 1개 추가 (→ 2^21 로 확장 시작):
  Hash table 0 (main hash table):       table size 1048576   number of elements 1048576
  Hash table 1 (rehashing target):      table size 2097152   number of elements 1            ← 새 키는 ht[1] 로

그 뒤 GET 3번만 실행:
  Hash table 0: number of elements 1048571                ← 5개가 ht[1] 으로 이동 (요청마다 조금씩)
  Hash table 1: number of elements 6

activerehashing yes 로 켜고 4초 후:
  Hash table 0: 462,076   Hash table 1: 586,501         ← cron 이 계속 이전 중 (총합 1,048,577 유지)
```
- **`activerehashing no` 에서는 명령이 오지 않으면 진행되지 않는다** → 옮기는 일이 **요청에 얹혀서(piggyback)** 진행된다는 증거.
- **수축(shrink)** ✅: 100만 키를 삭제하고 키 1개만 남기자 **`table size: 4`** 로 축소(요소 수가 버킷의 약 1/8 이하이면 축소 — 문서 기준, 정확한 임계는 미확인).
- 총합이 항상 **ht[0] + ht[1] = 전체 키 수**로 유지되어 **정합성은 깨지지 않는다.**

### 4-4. 실무 시사점

| 포인트 | 설명 |
|---|---|
| **메모리 일시 증가** | 확장 중 **ht[0] + ht[1] 이 공존** → 포인터 배열이 최대 **1.5배(옛 N + 새 2N)**. 큰 DB 에서 수백 MB 급(문서 기준) |
| **fork(BGSAVE) 와 충돌** | 자식 프로세스가 있는 동안 rehash 는 **COW 폭증을 막기 위해 억제**(`dict_can_resize`)되며, 비율이 **약 4:1 이상**으로 심해지면 강제 확장(문서 기준, 미재현) |
| **확장 시점 지연** | rehash 를 한 번에 안 하므로 **지연 스파이크는 없지만** 완료까지 **`SCAN` 이 두 테이블을 모두 순회**해야 해서 약간 느릴 수 있음 |
| **`expires` 도 별도 dict** | Q2 의 `expires` 테이블도 같은 방식 (`DEBUG HTSTATS` 의 `[Expires HT]`) ✅ |
| **Hash/Set/ZSet 의 내부 dict** | 큰 Hash(hashtable)/Set/ZSet 의 dict 도 동일한 점진적 rehash |

---

## Q5. 자료구조 인코딩 전환 — listpack/hashtable/quicklist/skiplist

### 5-1. 개념

Redis 는 **데이터가 작을 땐 메모리 효율적인 압축 구조(listpack/intset)**, **커지면 O(1)/O(log N) 구조(hashtable/skiplist/quicklist)** 로 **자동 변환**한다. 사용자는 보이지 않지만 `OBJECT ENCODING key` 로 볼 수 있다.

### 5-2. 임계값 (`CONFIG GET`, ✅ 7.2.6 기본값)

| 타입 | 압축 인코딩 → 일반 인코딩 조건 | 기본값 |
|---|---|---|
| **String** | `int`(정수) / `embstr`(짧은 문자열) / `raw` | embstr 는 **44바이트 이하** |
| **Hash** | listpack → hashtable | `hash-max-listpack-entries 512`, `hash-max-listpack-value 64` |
| **List** | listpack → quicklist | `list-max-listpack-size -2` (**노드당 8KB**) |
| **Set** | intset → listpack → hashtable | `set-max-intset-entries 512`, `set-max-listpack-entries 128`, `set-max-listpack-value 64` |
| **ZSet** | listpack → skiplist(+hashtable) | `zset-max-listpack-entries 128`, `zset-max-listpack-value 64` |

(**둘 중 하나라도** 초과하면 변환: 개수 초과 **또는** 값 길이 초과)

### 5-3. 실험 ✅ 검증

**String**
| 값 | 인코딩 | MEMORY USAGE |
|---|---|---|
| `"12345"` | **int** | 56 B |
| `a` × 44 | **embstr** | 104 B |
| `a` × 45 | **raw** | 120 B |
| `"9"` × 19 (long long 범위 밖) | embstr | 88 B |
| 정수 `10` 에 `APPEND x` | **raw** (수정 가능 문자열이 되면 raw) | — |

**Hash** (기본 `entries=512`)
| 필드 수 | 인코딩 | 메모리 |
|---|---|---|
| 512 | **listpack** | 4,664 B |
| **513** | **hashtable** | **37,032 B** |
| 5개 필드 + **65B 값** 1개 | **hashtable** (값 길이 64 초과) | — |
| 513 → 13 개로 삭제 | **hashtable 유지** (자동 되돌림 없음) | 10,056 B |

**ZSet** : 128 멤버 **listpack**(984 B) → **129 멤버 skiplist**(15,342 B, 약 **15배**). 129 → 10 개로 줄여도 **skiplist 유지** ✅.

**Set** : 정수 512 개 **intset**(1,592 B) → **513 개 hashtable**(28,824 B). 문자열 128 개 **listpack** → **129 개 hashtable**. 정수 집합에 문자열 추가(`abc`) → **listpack** 으로 전환(정수 → 일반 문자열 혼합).

**List** : 작은 요소 10 개 **listpack**. **1KB × 7 개 listpack**, **9 개 quicklist** (노드 8KB 한도를 7~9 개 사이에서 넘음). 9 개 → 2 개로 pop 해도 **quicklist 유지** ✅(이 실험에서).

### 5-4. 핵심 관찰

1. **전환은 한 방향이다**: 크기가 줄어도 **되돌아가지 않는다**(hash/zset/list 실험 모두). 한 번 커졌던 키는 **계속 큰 구조의 메모리 비용**을 지불 → 큰 컬렉션을 작게 되돌려 쓸 때는 **키를 새로 만들어 교체**.
2. **메모리 vs 속도 트레이드오프** ✅ (400 필드 Hash):
   - listpack: **4,664 B** vs hashtable: **28,656 B** — **약 6배 차이**
   - 속도: 마지막 필드 `HGET` 20만 번(파이프라인): **listpack 0.70초 vs hashtable 0.19초 (약 3.7배 느림)** — listpack 은 **순차 탐색 O(N)** 이라 개수가 늘수록 불리.
   - 그래서 임계값(512, 128)이 **"작을 때는 O(N)이어도 캐시 친화적·메모리 절약이 이득"** 이라는 균형점.
3. **임계값을 올리면** 메모리는 줄지만 **CPU(순차 탐색)·`fork` COW·big key 지연**이 늘 수 있다(`hash-max-listpack-entries` 를 수천으로 올리는 것은 위험).
4. **메모리 수치 주의**: `MEMORY USAGE` 는 **allocator 반올림/오버헤드가 포함**된 추정값이며(이 빌드는 libc), 정확한 바이트가 아니라 **상대 비교용**이다.

---

## Q6. maxmemory eviction 내부 — 근사 LRU/LFU

### 6-1. "정확한 LRU" 가 아니다

정확한 LRU 는 **모든 키를 접근 순서 리스트**로 유지해야 해서 메모리·CPU 비용이 크다. Redis 는 **키마다 24비트 `lru` 필드**(최근 접근 시각)만 두고, **eviction 때 무작위 N개를 샘플링해 그중 가장 오래된 것을 삭제**하는 **근사 LRU** 를 쓴다.

```
maxmemory 도달 → performEvictions():
   repeat:
      샘플 N개(maxmemory-samples, 기본 5) 를 키 공간에서 무작위 추출 (+ eviction pool 로 이전 후보와 비교)
      그중 idle(미사용 시간)이 가장 큰 키 삭제
   until 메모리 < maxmemory
```

### 6-2. 실험 ✅ 검증 — 샘플 수와 정확도

20,000 개 키 중 **90%는 최근 접근(hot)**, **10%는 2초 이상 idle(cold)** 로 만든 뒤 `maxmemory` 를 낮춰 eviction 을 유발했다. **정확한 LRU 라면 cold 만 삭제**되어 **hot 삭제 비율이 0%** 여야 한다.

| `maxmemory-samples` | 삭제된 hot | 삭제된 cold | **hot 이 삭제된 비율** |
|---|---|---|---|
| 1 | 1,680 | 176 | **90.5%** (사실상 무작위) |
| 3 | 1,338 | 502 | 72.7% |
| **5 (기본)** | 1,122 | 720 | **60.9%** |
| 10 | 663 | 1,199 | 35.6% |
| 20 | 272 | 1,600 | **14.5%** |

- 샘플이 클수록 **정확한 LRU 에 근접**하지만 **CPU 비용 증가**. 기본 5 는 **속도와 정확도의 절충**(공식 문서는 10 이 거의 진짜 LRU 에 가깝다고 설명).
- (참고) 두 번째 실험에서 hot 이 50% 일 때는 샘플 5 에서 hot 삭제 0% 였다 — **cold 비율이 높을수록 샘플에 cold 가 포함될 확률이 높아져** 정확도가 좋아 보인다. **hot 이 대부분인 워크로드(캐시 hit 율이 높은 경우)에서 근사 오차가 크다.**
- **개선(문서 기준)**: Redis 3.0 부터 **eviction pool**(이전 샘플의 좋은 후보를 보관해 비교)로 정확도 향상.

### 6-3. LFU (Least Frequently Used)

`maxmemory-policy allkeys-lfu` / `volatile-lfu`. 키의 24비트 필드를 **상위 16비트 = 마지막 감소 시각(분), 하위 8비트 = 로그 접근 카운터(0~255)** 로 쓴다.

✅ 검증 (`lfu-log-factor 10`, `lfu-decay-time 1`):

| 접근 횟수 | `OBJECT FREQ` 카운터 |
|---|---|
| 새 키 | **5** (초기값 `LFU_INIT_VAL`) |
| 10 | 7 |
| 100 | 12 |
| 1,000 | 16 |
| 10,000 | 54 |
| 100,000 | 138 |
| 1,000,000 | **255** (포화) |
| `lfu-log-factor 0` 으로 300번 | **255** (바로 포화) |

- 카운터는 **확률적으로 증가**(현재 값이 클수록 더 드물게 증가) → **8비트로 100만 회 이상을 표현**. `lfu-log-factor` 가 클수록 **느리게 포화**한다.
- **`lfu-decay-time`(분)** 마다 카운터가 감소 → 한때 인기였던 키도 **시간이 지나면 점수가 떨어진다**.
- LFU 는 **일시적으로 몰린 접근(스캔 등)에 덜 흔들린다**는 장점(LRU 는 한 번 읽은 키를 최근 사용으로 간주).
- **LFU 정책이 아니면 `OBJECT FREQ` 는 에러** ✅: `ERR An LFU maxmemory policy is not selected, access frequency not tracked...` (정책 전환 직후엔 LRU/LFU 데이터가 한동안 부정확).
- `OBJECT IDLETIME` ✅: 3초 idle 후 `3`, `GET` 후 `0`. (LRU 는 초 단위 해상도의 시계(`LRU_CLOCK`)를 사용 — 문서 기준)

### 6-4. 정리

| 항목 | 설명 |
|---|---|
| 방식 | **샘플링 기반 근사** (정확한 전역 순서 유지 안 함) |
| 정확도 튜닝 | `maxmemory-samples` ↑ → 정확 ↑, CPU ↑ (✅ 위 표) |
| LRU vs LFU | **최근성** vs **빈도** — 반복 접근 패턴/스캔 노이즈가 많으면 LFU |
| 키당 비용 | 24비트(약 3바이트) — 객체 헤더에 포함 |
| 주의 | eviction 은 **메인 스레드에서 쓰기 명령 처리 전에** 수행 → 쓰기가 많고 메모리가 항상 한계면 **지연 증가**(`INFO` 의 `total_eviction_exceeded_time`, `current_eviction_exceeded_time`) |
| `volatile-*` | TTL 있는 키만 후보. 없으면 `noeviction` 처럼 OOM(13장) |

---

## Q7. MULTI/EXEC 와 WATCH 내부

### 7-1. 구조

```
MULTI            → 클라이언트에 "트랜잭션 상태" 플래그 (CLIENT LIST 에서 multi=N, flags=x)
명령들            → 즉시 실행하지 않고 클라이언트의 명령 큐에 쌓음, 응답은 +QUEUED
EXEC             → 큐의 명령을 메인 스레드에서 연속 실행 (중간에 다른 클라이언트 명령이 끼지 않음)
```

### 7-2. 실험 ✅ 검증

| # | 시나리오 | 결과 |
|---|---|---|
| 1 | **큐잉 중 문법 오류**(`BADCMD`) 후 `EXEC` | **`-EXECABORT Transaction discarded because of previous errors.`** — 큐 전체 취소, 앞의 `SET x 2` 도 반영 안 됨 (`GET x` → 원래 값 `1`) |
| 2 | **실행 중 오류**(문자열에 `INCR`) | `EXEC` 응답 `*3: +OK, -ERR value is not an integer..., +OK` — **오류 명령만 실패하고 앞뒤 명령은 모두 반영** (`y=10`, `z=30`) → **롤백 없음** |
| 3 | 큐잉 중 다른 클라이언트 | 큐잉만 한 상태에서는 **키가 변하지 않음**(`GET cnt` → nil). `EXEC` 시 연속 실행(`:1, :2`) |
| 4 | **`WATCH w` 후 다른 클라이언트가 `SET w`** | `EXEC` → **`*-1` (nil)** — **트랜잭션 중단**, 값은 다른 클라이언트의 값 유지 |
| 5 | 다른 클라이언트가 **같은 값으로** `SET w 5` | `EXEC` → **nil (중단)** — **값 비교가 아니라 "쓰기 발생"을 감지** |
| 6 | `WATCH` 한 키가 **TTL 로 만료**(Active expire 가 삭제) | `EXEC` → **nil (중단)** — 만료 삭제도 변경으로 취급 |
| 7 | 〃 만료 후 다른 클라이언트가 `GET`(Lazy 삭제) | `EXEC` → nil |
| 8 | `UNWATCH`, `DISCARD` | `OK` (WATCH 해제 / 큐 폐기) |
| 9 | `CLIENT LIST` | `multi=1 flags=x` (큐에 쌓인 명령 수 표시) |
| 10 | **`MULTI` 안의 `BLPOP`** | **블록하지 않고** 0.06초 만에 `EXEC` 응답 `*1 *-1`(nil) |

### 7-3. 왜 롤백이 없는가 / 동작 원리

1. **설계 철학**: Redis 명령의 에러는 **프로그래밍 오류**(잘못된 문법/타입)이므로 **개발 단계에서 잡혀야 한다**고 보고, **롤백 구현의 복잡도·성능 비용**을 피한다(공식 문서 설명).
2. **EXEC 의 원자성은 "격리 + 전부 실행"**: 한 번 시작한 큐는 **다른 클라이언트가 끼어들지 않고** 끝까지 실행된다(싱글 스레드). 중간 오류가 나도 **계속 진행**한다(#2).
3. **큐잉 단계 오류 vs 실행 단계 오류**: 큐잉 때 **문법·인자 수·알 수 없는 명령**을 검사해 오류를 기록하면 `EXECABORT`(#1). **타입 불일치처럼 실행해야 아는 오류**는 롤백 불가(#2).
4. **WATCH = 낙관적 락(CAS)**: 서버가 DB 의 **`watched_keys`**(키 → WATCH 한 클라이언트 목록)를 유지. **어떤 쓰기든 `signalModifiedKey` 로 해당 키를 WATCH 한 클라이언트에 `DIRTY_CAS` 플래그**를 세움(#4, #5, #6). `EXEC` 시 플래그가 있으면 **nil 반환 + 큐 폐기**. **같은 값이라도 쓰기면 플래그**(#5).
5. **`EXEC`/`DISCARD`/`UNWATCH`/연결 종료 시 WATCH 가 해제**된다.
6. **AOF/복제**: `EXEC` 는 **`MULTI … EXEC` 묶음으로 AOF/replica 에 전파**(Q12 ✅ 스크립트의 다중 쓰기 AOF 기록과 같은 형태).

### 7-4. 실무 포인트

| 포인트 | 설명 |
|---|---|
| **조건부 갱신** | `WATCH key` → `GET` → 계산 → `MULTI/SET/EXEC` → **nil 이면 재시도** (CAS 루프). 경합이 심하면 **Lua/Function** 으로 서버 측 원자 처리가 더 간단하고 빠름 |
| **오류 처리** | `EXEC` 반환 배열의 **각 응답을 검사**(일부만 실패 가능, 파이프라인과 같은 이유 — 12장) |
| **클러스터** | 트랜잭션의 모든 키가 **같은 슬롯**(해시태그) |
| **큐잉 중 연결 점유** | `MULTI` 후 `EXEC` 를 안 보내면 클라이언트 상태/메모리 유지(`multi-mem`) — 타임아웃/누수 주의 |
| **만료 키와 WATCH** | 위 #6: **WATCH 중 만료도 abort** 사유가 됨 — 만료가 임박한 키를 WATCH 하면 재시도가 늘 수 있음 |

---

## Q8. 블로킹 명령(BLPOP, WAIT)은 어떻게 서버를 멈추지 않고 대기하나

### 8-1. 핵심

**"블로킹" 은 서버 스레드가 아니라 클라이언트의 요청 하나가 대기하는 것**이다. 서버는 해당 클라이언트를 **"블록 상태로 표시하고 응답을 보류"** 한 채 **즉시 다른 요청 처리를 계속**한다.

```
클라이언트 A: BLPOP q 0
   메인 스레드: q 가 비어 있음 → A 를 "q 를 기다리는 클라이언트" 로 등록(db->blocking_keys[q] 큐), 응답 보류, 다음 이벤트 처리
클라이언트 B: RPUSH q x
   메인 스레드: 키에 데이터가 들어옴 → db->ready_keys 에 q 추가 → (명령 처리 후 handleClientsBlockedOnKeys)
                q 를 기다리던 A 를 깨워 LPOP 실행 → A 에게 응답
```

### 8-2. 실험 ✅ 검증

| # | 시나리오 | 결과 |
|---|---|---|
| 1 | 같은 키에 `BLPOP q 0` 3개 | `INFO clients`: `blocked_clients:3`, `total_blocking_keys:1`. `CLIENT LIST` 에 **`flags=b cmd=blpop`** |
| 2 | 블록된 동안 다른 명령 | **`PING`, `SET` 정상 응답** — 서버는 멈추지 않음 |
| 3 | `RPUSH q a b c` | **대기 순서(FIFO)대로** 클라이언트0→`a`, 1→`b`, 2→`c` |
| 4 | `BLPOP empty 1` | **정확히 1.00초 후 nil** (타임아웃) |
| 5 | `CLIENT UNBLOCK <id>` | `1` 반환, 대기 클라이언트는 nil(타임아웃과 동일) |
| 6 | `CLIENT UNBLOCK <id> ERROR` | 대기 클라이언트가 **`-UNBLOCKED client unblocked via CLIENT UNBLOCK`** 에러 수신 |
| 7 | `BLPOP tk` 대기 중 **`tk` 를 문자열로 `SET`** | 클라이언트는 **계속 블록 유지**(`blocked_clients:1`) — 타입이 리스트가 될 때까지 대기 |
| 8 | `WAIT 1 500` (레플리카 없음) | **0.51초 대기 후 `0` 반환** |

### 8-3. 정리

- **서버 메인 스레드는 블록되지 않는다**(#2). 블록되는 건 **그 클라이언트의 응답**뿐.
- **FIFO 공정성**: 같은 키를 기다리는 클라이언트는 **등록 순서대로** 깨운다(#3).
- **`BLPOP 0` 은 무한 대기** → 소비자 장애/네트워크 단절 시 **서버는 연결이 끊긴 것을 감지할 때까지 `blocked_clients` 로 유지**. `tcp-keepalive`, 클라이언트 타임아웃 설정이 중요(12장).
- **`WAIT`/`WAITAOF`** 도 같은 방식(클라이언트를 블록 상태로 두고 레플리카 ACK 도착 시 깨움).
- **MULTI/Lua 안에서는 블록하지 않는다**(Q7 #10, Q12 ✅): 원자성을 유지하려고 블로킹 명령은 **타임아웃 없이 즉시 nil** 처럼 동작.
- 운영 지표: `blocked_clients`(13장 필수 패널 #7), `total_blocking_keys`, `CLIENT LIST` 의 `flags=b`.
- 블로킹 소비자가 많은 키는 **한 개의 `RPUSH` 에 여러 클라이언트가 연달아 깨어남** → 처리 순서는 FIFO 지만 **한 배치가 메인 스레드를 점유**할 수 있음(문서 기준).

---

## Q9. 복제 내부 — replid, offset, backlog, PSYNC

8장에서 **흐름**을 정리했고, 여기서는 **식별자·오프셋·backlog 가 실제로 어떻게 움직이는지**를 실측으로 확인한다.

### 9-1. 핵심 개념 (`INFO replication` ✅)

| 필드 | 의미 |
|---|---|
| `master_replid` | **이 데이터셋의 히스토리 ID**(40자 hex). 마스터가 바뀌면(승격) **새로 생성** |
| `master_replid2` | **이전 히스토리 ID**(승격 직전의 replid). 옛 마스터의 다른 레플리카가 **부분 재동기화**할 수 있게 보관 (PSYNC2) |
| `master_repl_offset` | **복제 스트림의 누적 바이트 오프셋**. 쓰기 명령이 전파될 때마다 증가 |
| `slave_repl_offset` (레플리카) | **레플리카가 처리한 오프셋**. 마스터와 같으면 **완전 동기화** |
| `slaveN:...state=online,offset=...,lag=` (마스터) | 마스터가 본 각 레플리카의 ACK 오프셋과 지연(초) |
| `repl_backlog_size` / `histlen` / `first_byte_offset` | **backlog(링 버퍼)의 설정 크기, 실제 보관 길이, 보관 중인 가장 오래된 오프셋** |

### 9-2. 오프셋은 "RESP 바이트 수" ✅ 검증

```
SET bb 22  →  master_repl_offset +29    (레플리카 slave_repl_offset 도 동일하게 +29, 값 일치)
```
- 복제 스트림은 **명령을 RESP 로 직렬화한 바이트열**이고 오프셋은 **그 누적 바이트**다. 같은 오프셋이면 **같은 데이터**(replid 가 같다는 전제).
- **마스터는 10초마다 레플리카에 `PING` 을 전파**한다(`repl-ping-replica-period 10`) ✅: 11초간 오프셋 **+14 bytes**(`*1\r\n$4\r\nPING\r\n`). 쓰기가 없어도 오프셋이 천천히 증가하는 이유이자, **레플리카가 마스터 생존을 확인하는 heartbeat**.
- 레플리카는 **`REPLCONF ACK <offset>` 을 1초마다** 마스터에 보낸다(문서 기준). 마스터는 이를 `slaveN offset/lag` 로 기록하고 `WAIT`/`min-replicas` 판단에 사용.

### 9-3. PSYNC 판정 — 부분 vs 전체 재동기화 ✅ 검증

레플리카는 재연결 시 **`PSYNC <replid> <offset+1>`** 을 보낸다. 마스터는 아래를 만족하면 **부분 재동기화**, 아니면 **`FULLRESYNC`**.

```
(요청 replid == 마스터 replid  또는  == master_replid2 (이때 offset ≤ second_repl_offset))
 AND 요청 offset 이 backlog 범위 안 ([repl_backlog_first_byte_offset, master_repl_offset])
```

| 실험 | 결과 | 의미 |
|---|---|---|
| **(a)** 마스터가 레플리카 연결을 `CLIENT KILL TYPE REPLICA` 로 종료, 즉시 재연결 | **`sync_partial_ok` +1**, 로그 `Successful partial resynchronization with master.` | replid 동일 + offset 이 backlog 안 → **끊긴 구간만 전송** |
| **(b)** 레플리카를 **`REPLICAOF NO ONE` → 다시 `REPLICAOF`**(승격 후 되돌림), 사이에 소량 쓰기 | **`sync_partial_err` +1 → `sync_full` +1**, 로그 `Trying a partial resynchronization (request 2c109890...:95)` | `NO ONE` 이 **레플리카의 replid 를 새로 만들어** 요청 replid 가 마스터에 **알려지지 않은 값** → **부분 재동기화 불가, 전체 동기화** |
| **(c)** 레플리카 `SIGSTOP`, 마스터가 연결 종료, **backlog(16KB)보다 많은 쓰기(약 420KB)** 후 `SIGCONT` | **`sync_partial_err` +1 → `sync_full` +1**. `request ...:6213` 인데 `backlog first_byte_offset=387689` → 요청 오프셋이 **backlog 에서 밀려남** | backlog 초과 → **전체 동기화** (`Full resync from master: ...:420125`) |
| **(d)** (c)와 같은 방식, **소량 쓰기(약 6KB < backlog)** | **`sync_partial_ok` +1** | backlog 안 → 부분 |
| 데이터 일관성 | 마스터/레플리카 `DBSIZE` **5352 = 5352** | 부분/전체 모두 최종 일치 |

- `PSYNC ? -1`(nc 로 직접 호출) → **`+FULLRESYNC <replid> <offset>`** ✅ — 처음 붙는 레플리카에 대한 응답 형식.
- 지표: `INFO stats` 의 **`sync_full`, `sync_partial_ok`, `sync_partial_err`** (13장 권장 패널 #21) — **`sync_full` 이 반복 증가하면 backlog 부족/네트워크 불안정**.

### 9-4. backlog 크기 관찰 ✅

`repl-backlog-size 16384` 로 설정했는데도 **`repl_backlog_histlen:32437`** (설정의 약 2배)로 보관되었다. **7.0+ 의 backlog 는 "공유 복제 버퍼(shared replication buffer)" 의 블록 단위(16KB)로 관리**되어 **설정 크기를 블록 단위로 올림**하고, **레플리카가 쓰고 있는 블록은 backlog 한도 이상 보관**될 수 있다(문서 기준). 따라서 **backlog 크기는 "하한 보장값"** 으로 이해하는 편이 안전하다.

### 9-5. 설계 시사점

| 포인트 | 설명 |
|---|---|
| **backlog 크기 산정** | **허용 단절 시간 × 초당 쓰기 바이트** 이상 (예: 60초 단절 허용 × 5MB/s 쓰기 → 300MB). 기본 1MB 는 대부분 부족 |
| **failover 후 부분 동기화** | 승격된 새 마스터가 **옛 replid 를 `replid2` 로 보관**해 다른 레플리카가 **전체 동기화 없이** 따라올 수 있음(PSYNC2, 9장 ✅ `Setting secondary replication ID`) |
| **`REPLICAOF NO ONE` 후 되돌리기 주의** | (b) 처럼 **전체 동기화**가 발생(데이터가 큰 경우 부하) — 승격 후 되돌릴 때 유의 |
| **full sync 폭주** | backlog 부족 + 불안정 네트워크 → full sync → fork → 메모리/지연 → 더 불안정 (악순환) |
| **복제 지연 모니터링** | `master_repl_offset - slave_repl_offset`, `slaveN lag` (13장 패널 #8) |

---

## Q10. Stream 내부 — radix tree + listpack, Consumer Group PEL

### 10-1. 구조 (문서 기준 + 실측 확인)

```
Stream = Radix Tree (rax)  :  key = 노드의 첫 entry ID(128bit = ms-seq),   value = listpack(여러 entry 를 묶은 노드)

  rax ── "1792331361795-0" → listpack[ master entry(필드 목록) | entry1 | entry2 | ... (최대 100개 / 4096B) ]
      ── "1792331361900-0" → listpack[ ... ]

Consumer Group = 별도 rax:  PEL(Pending Entries List: 전달됐지만 XACK 안 된 entry ID → 소비자, 전달 시각, 전달 횟수)
```

- **하나의 listpack 노드에 여러 entry** 를 담고 **필드 이름은 노드의 master entry 에 한 번만 저장**(이후 entry 는 "같은 필드" 플래그로 값만 저장) → **메모리 효율**.
- 새 노드는 **`stream-node-max-entries 100` 또는 `stream-node-max-bytes 4096` 에 도달**하면 생성.

### 10-2. 실험 ✅ 검증

설정: `stream-node-max-bytes 4096`, `stream-node-max-entries 100`, 인코딩 `OBJECT ENCODING` = **`stream`**. entry 는 `a 1 b 2` (필드 2개):

| entries | radix-tree-keys (= listpack 노드 수) | radix-tree-nodes | MEMORY USAGE | 1 entry 당 |
|---|---|---|---|---|
| 1 | 1 | 2 | 4,736 B | — |
| 100 | **1** | 2 | 4,736 B | 47.4 B |
| **101** | **2** | 6 | 7,264 B | 71.9 B |
| 250 | 3 | 8 | 9,304 B | 37.2 B |
| 1,000 | 10 | 22 | 23,584 B | 23.6 B |
| 10,000 | **100** | 186 | 203,280 B | **20.3 B** |

- **100 개마다 노드(listpack)가 하나씩 늘어남**(entries 100→101 에서 keys 1→2) ✅ — `stream-node-max-entries` 의 동작.
- entry 가 늘수록 **1 entry 당 메모리가 감소**(고정 오버헤드 분산, 필드 이름 공유) — 같은 값 10,000 개 `RPUSH`한 List(`a1b2`) 는 **65,952 B(6.6 B/entry)** 로 더 작지만 **필드가 없는 단순 값**이라 직접 비교는 부적절(Stream 은 ID·필드 구조 포함).
- **ID 규칙** ✅: `ms-seq`, **항상 마지막 ID 보다 커야 함** — `XADD ... 1-1`(과거 ID)은 `ERR The ID specified in XADD is equal or smaller than the target stream top item`. `ms-*` 처럼 **seq 만 자동** 지정하는 형식도 있다(문서 기준, 미실행). 시계가 뒤로 가도 단조 증가를 보장(문서 기준).
- `XTRIM MAXLEN 1` 로 오래된 entry 삭제 → `XLEN 1`.

### 10-3. Consumer Group 과 PEL ✅ 검증

```
XGROUP CREATE q g 0           # 그룹 생성 (0: 처음부터)
XREADGROUP GROUP g c1 COUNT 3 STREAMS q '>'      # c1 에게 3개 전달 (">" = 아직 아무에게도 안 간 것)
→ 전달된 entry 가 PEL 에 기록

XPENDING q g                   → 3  (최소ID) (최대ID)  c1 3         ← 요약: 개수, 범위, 소비자별 개수
XPENDING q g - + 10            → [ID, 소비자, idle(ms), 전달 횟수] ...   ← 상세 (idle=11ms, 전달 횟수 1)
XACK q g <id>                  → pending 3 → 2                       ← ACK 하면 PEL 에서 제거
XAUTOCLAIM q g c2 1000 0       → idle ≥ 1000ms 인 pending 을 c2 가 가져감 (소유권 이전)
XINFO GROUPS q                 → consumers 2, pending 2, last-delivered-id, entries-read 3, lag 2
XINFO CONSUMERS q g            → c1 pending 0 idle 1268 / c2 pending 2 idle 22
새 consumer c3 로 '>'          → 아직 전달 안 된 2개만 (이미 c1 이 받은 건 안 감)
c3 에게 '0'                    → "내 PEL" (방금 받은 것) 조회 — 재시작 후 미처리분 재확인용
```

- **PEL 이 at-least-once 의 핵심**: ACK 전에 소비자가 죽으면 **PEL 에 남아** 있다가 **`XAUTOCLAIM`/`XCLAIM`** 으로 다른 소비자가 인수(6장의 List 신뢰 큐(BLMOVE)보다 구조적으로 지원).
- **`>` vs ID**: `>` 는 **새 메시지**, `0`/구체 ID 는 **자기 PEL 재조회**.
- **`lag`**(7.0+): 그룹이 아직 읽지 않은 entry 수 → **소비 지연 모니터링**에 사용.
- 메모리 주의: **ACK 하지 않은 PEL 은 계속 쌓임** + **스트림 entry 는 소비해도 자동 삭제되지 않음**(`XTRIM`/`MAXLEN`/`MINID` 필요) — 6장의 운영 포인트.

---

## Q11. RESP 프로토콜과 reply 경로

### 11-1. RESP2 타입 ✅ 검증 (소켓으로 직접 송수신)

첫 바이트가 타입을 결정한다. 모든 항목은 `\r\n` 으로 끝난다.

| 첫 글자 | 타입 | 예 (실제 응답) |
|---|---|---|
| `+` | Simple String | `SET k v` → `+OK\r\n` |
| `-` | Error | `FOOO` → `-ERR unknown command 'FOOO', with args beginning with: \r\n`, `LPUSH k x`(타입 불일치) → `-WRONGTYPE Operation against a key holding the wrong kind of value\r\n` |
| `:` | Integer | `INCR n` → `:1\r\n`, `RPUSH l a b` → `:2\r\n` |
| `$` | Bulk String (길이 접두) | `GET k` → `$1\r\nv\r\n`, **nil → `$-1\r\n`** |
| `*` | Array | `LRANGE l 0 -1` → `*2\r\n$1\r\na\r\n$1\r\nb\r\n`, 빈 배열 `*0\r\n`, **배열 안의 nil** `MGET a zz b` → `*3\r\n$1\r\n1\r\n$-1\r\n$1\r\n2\r\n` |

**요청도 같은 포맷**: `*3\r\n$3\r\nSET\r\n$3\r\nfoo\r\n$3\r\nbar\r\n` (배열 of Bulk String).

### 11-2. 프로토콜 특성 ✅

| 특성 | 확인 |
|---|---|
| **인라인 명령** | `PING\r\n` 만 보내도 `+PONG` — `telnet`/`nc` 로 테스트 가능(공백 구분, 따옴표 처리) |
| **바이너리 안전(길이 기반 파싱)** | 값에 `\r\n` 이 들어 있어도(`$6\r\na\r\nb\r\nc\r\n`) **정상 저장/조회**: `GET` → `$6\r\na\r\nb\r\nc\r\n` — **구분자가 아니라 길이**를 읽기 때문 |
| **파이프라이닝** | 한 번에 `PING` 3개를 보내면 응답 `+PONG\r\n+PONG\r\n+PONG\r\n` 이 **순서대로** (12장) |
| **TCP 분할 내성** | 명령을 **`PI` / `NG\r\n` 두 조각**으로 나눠 보내도 정상 처리 — 서버는 **query buffer 에 누적해 완성된 명령만 파싱** |
| **프로토콜 오류** | `*1\r\n$abc\r\n` → `-ERR Protocol error: invalid bulk length` (이후 서버가 연결을 닫는다는 것은 문서 기준, 미확인) |
| **응답 순서 = 요청 순서** | 파이프라인/pub-sub 푸시를 제외하면 한 연결은 **FIFO** |

### 11-3. RESP3 (`HELLO 3`) ✅ 검증

| RESP2 | RESP3 | 예 |
|---|---|---|
| nil 이 `$-1` / `*-1` | **전용 null `_`** | `GET missing` → `_\r\n` |
| `HGETALL` 이 **평탄 배열**(`*2 f v`) | **Map `%`** | `%1\r\n$1\r\nf\r\n$1\r\nv\r\n` |
| `SMEMBERS` 가 배열 | **Set `~`** | `~1\r\n$1\r\na\r\n` |
| 점수 등이 **문자열** | **Double `,`** | `ZSCORE z m` → `,1.5\r\n` |
| — | Boolean `#`, Verbatim `=`, BigNumber `(`, **Push `>`** | **Push** 는 서버가 요청 없이 보내는 메시지(클라이언트 사이드 캐싱의 `invalidate`, 12장 ✅) |

- `HELLO 3` 응답 자체가 **Map**(`%7 ... server redis version 7.2.6 proto 3 id ... mode ...`).
- RESP3 의 **Push 타입** 덕에 **같은 연결에서** Pub/Sub 메시지/invalidate 를 일반 응답과 구분해 받을 수 있다(RESP2 는 구독 모드 연결을 따로 써야 함).

### 11-4. reply 경로 (서버 내부, 문서 기준)

```
addReply* → 고정 16KB client->buf 에 쓰고, 넘치면 client->reply 리스트(청크)에 연결     ← CLIENT LIST 의 obl(고정 버퍼) / oll(리스트 길이) / omem (12장 ✅)
          → 클라이언트를 "pending write" 큐에 등록
beforeSleep → handleClientsWithPendingWrites → write() 시도(대부분 한 번에 끝남)
            → 소켓이 가득 차면 EPOLLOUT/kqueue 쓰기 가능 이벤트를 등록해 나중에 이어서 전송
```
- 응답이 **클라이언트가 읽는 속도보다 빠르게 쌓이면** reply 리스트가 커져 `client-output-buffer-limit` 에 걸린다(12장 ✅).
- I/O 스레드(Q1) 는 이 **write 단계(와 read 단계)** 를 분담.

---

## Q12. Lua/Function 실행 모델

### 12-1. 기본 동작 ✅ 검증

| 항목 | 결과 |
|---|---|
| `EVAL "return {KEYS[1], ARGV[1], redis.call('SET', KEYS[1], ARGV[1])}" 1 mykey val` | `mykey val OK` — `KEYS`/`ARGV` 로 입력 전달 |
| **원자성** | 스크립트는 **메인 스레드에서 처음부터 끝까지 한 덩어리**로 실행, 중간에 다른 명령이 끼지 않음(13장 ✅: 장시간 스크립트가 서버를 막음 — `BUSY`) |
| **스크립트 캐시** | `SCRIPT LOAD` → SHA1 반환, `EVALSHA <sha>` 로 재사용, `SCRIPT EXISTS` 로 확인. **`EVAL` 도 실행하면 자동 캐시**됨(`script exists` → `1`). 없는 SHA → **`NOSCRIPT No matching script. Please use EVAL.`** |
| **캐시 휘발성** | `DEBUG RELOAD` 후에도 `SCRIPT EXISTS` 는 `1` 이었지만 **실제 재시작(`SHUTDOWN SAVE` → 재기동) 후 `0`** ✅ — 스크립트 캐시는 **RDB/AOF 에 저장되지 않음** → 클라이언트는 `NOSCRIPT` 시 `EVAL` 로 재전송하는 로직 필요 |
| 메모리 | `INFO memory`: `number_of_cached_scripts:4`, `used_memory_scripts_eval:640` — **스크립트를 계속 `EVAL` 로 다른 문자열로 보내면 캐시가 무한 증가**(리터럴 값을 스크립트에 박지 말고 `ARGV` 사용) |

### 12-2. 에러 처리 ✅

| 호출 | 동작 |
|---|---|
| `redis.call('INCR','e1')` (타입 오류) | **에러를 던져 스크립트 중단**: `ERR value is not an integer or out of range script: <sha>, on @user_script:1.` — **그 전에 실행된 `SET e1 x` 는 롤백되지 않음**(`GET e1` → `x`) |
| `redis.pcall(...)` | 에러를 **테이블로 반환**(`{err=...}`) → 스크립트가 계속 진행 가능 |
| 스크립트 안 `BLPOP nolist 5` | **블록하지 않고 즉시 nil** ✅ |
| 선언하지 않은 키 접근 (`KEYS` 에 없는 키 `GET`) | **단일 서버에서는 동작**하지만 **클러스터/ACL 에서 문제**(키는 반드시 `KEYS` 로 전달, 11장 ✅ 스크립트 내 ACL 키 검사) |

### 12-3. 복제/AOF — "effects replication" ✅ 검증

`appendfsync always` 서버에서 `EVAL "redis.call('SET','a','1'); redis.call('INCR','n2'); return 1" 0` 후 AOF(incr) 확인:

```
 MULTI
 SET a 1
 INCR n2
 EXEC
```
- **`EVAL` 문자열은 AOF 에 기록되지 않고(`grep EVAL` = 0건), 스크립트가 실제 실행한 쓰기 명령만 기록**된다 (**Redis 5.0+ 기본, 7.0 부터 유일한 방식**). 쓰기가 **2개 이상이면 `MULTI/EXEC` 로 묶어** 원자성 유지, 단일 쓰기면 묶지 않음(`SET cnt 1`).
- 덕분에 **비결정적 명령(`TIME`, `RANDOMKEY` 등)이 있어도 레플리카/AOF 재생 결과가 동일**하다(과거에는 스크립트를 그대로 전파해 결정성을 강제했음).
- `redis.call('TIME')` 같은 비결정적 명령도 사용 가능 ✅(`1`).

### 12-4. Function (7.0+) ✅ 검증

```
FUNCTION LOAD "#!lua name=mylib
redis.register_function('myfunc', function(keys, args) return redis.call('SET', keys[1], args[1]) end)
redis.register_function{function_name='myro', callback=function(keys,args) return redis.call('GET', keys[1]) end, flags={'no-writes'}}"
FCALL myfunc 1 fk fv      → OK
FCALL_RO myro 1 fk        → fv
FCALL_RO myfunc 1 fk x    → ERR Can not execute a script with write flag using *_ro command.
```

| 구분 | EVAL(스크립트) | **FUNCTION** |
|---|---|---|
| 저장 | 클라이언트가 보냄, **서버 캐시(휘발)** | **서버에 라이브러리로 등록**, **RDB/AOF 에 영속**(✅ `DEBUG RELOAD` 후 `FUNCTION LIST` 에 `mylib` 유지) |
| 호출 | `EVAL`/`EVALSHA` | `FCALL`/`FCALL_RO` (이름으로 호출) |
| 플래그 | 선택 | **`no-writes`, `allow-oom` 등을 선언** → `FCALL_RO`/replica 에서 안전하게 실행 |
| 배포 | 앱이 스크립트 관리 | **서버 측 코드 배포**(`FUNCTION LOAD/DELETE/DUMP/RESTORE`) |
| 복제 | effects 복제 | 함수 정의는 **복제됨**, 호출은 effects 복제 |
| 통계 | — | `FUNCTION STATS`(`libraries_count`, `functions_count`, 실행 중 스크립트) ✅ |

- 스크립트 캐시 휘발·`NOSCRIPT` 문제를 **영속 라이브러리**로 해결. 단 **서버에 코드가 존재**하므로 **버전 관리/배포 절차**가 필요(운영 부담).

### 12-5. 실행 모델 요약

```
EVAL → ① 스크립트 SHA1 계산·캐시(없으면 컴파일) → ② Lua VM(서버 내장, 샌드박스)에서 실행
        · redis.call/pcall → 같은 메인 스레드에서 명령 즉시 실행(ACL/키 검사 포함, 11장 ✅)
        · 전체 실행 시간 동안 다른 클라이언트 명령은 대기(13장 ✅ BUSY, busy-reply-threshold 5000ms 후 SCRIPT KILL/FUNCTION KILL 가능)
        · 쓰기 명령은 effects 로 AOF/replica 에 전파(✅ MULTI/EXEC 묶음)
```
- **쓰기를 한 스크립트는 `SCRIPT KILL` 불가**(이미 데이터가 바뀜): 이 경우 `SHUTDOWN NOSAVE` 만 가능(`BUSY ... You can only call SCRIPT KILL or SHUTDOWN NOSAVE` — 쓰기 후에는 `UNKILLABLE`, 문서 기준, 미재현).

---

## Q13. RDB 파일 포맷 해부

### 13-1. 구조 (문서 기준 + 실물 hexdump 로 확인)

```
[ "REDIS" + 4자리 버전 ]            52 45 44 49 53 30 30 31 31   = "REDIS0011"  (RDB 버전 11)
[ AUX 필드들 (0xFA) ]               key/value 문자열 쌍: redis-ver, redis-bits, ctime, used-mem, aof-base
[ DB 선택 (0xFE) + 크기 힌트(0xFB) ]  FE 00 = DB 0,   FB <키 수> <expire 키 수>
[ 키-값 항목 반복 ]                  [ (선택) expire: 0xFC + 8바이트 ms ]  [ 타입 1바이트 ] [ 키(문자열) ] [ 값 ]
[ EOF (0xFF) ]
[ CRC64 체크섬 8바이트 ]
```

### 13-2. 실물 해석 ✅ 검증 — 5개 키(171바이트)

`SET hello world`, `SET num 12345`, `SET ttlkey v PX 4102444800000`, `RPUSH lst a b`, `HSET h f v` 저장 후 `xxd dump.rdb`:

```
5245 4449 5330 3031 31      REDIS0011                                  ← 매직 + 버전
fa 09 "redis-ver" 05 "7.2.6"                                            ← AUX (0xFA, 길이 접두 문자열 2개)
fa 0a "redis-bits" c0 40                                                ← c0 = 8비트 정수 인코딩 (64)
fa 05 "ctime" c2 a4ce d46a                                              ← c2 = 32비트 정수
fa 08 "used-mem" c2 ...   fa 08 "aof-base" c0 00
fe 00                                                                   ← SELECT DB 0
fb 05 01                                                                ← 해시 테이블 크기 힌트: 키 5개, 그중 expire 1개
12 03 "lst" 01 02 0d 0d000000 02 00 81 61 02 81 62 02 ff               ← 타입 0x12 (List/QUICKLIST_2) , 키 "lst", 값(listpack 바이트열: a, b)
00 05 "hello" 05 "world"                                               ← 타입 0x00 (String), 키/값 모두 길이 접두 문자열
fc ab080b7c5c050000 00 06 "ttlkey" 01 "v"                              ← fc = 만료(밀리초 절대 시각, 8바이트 little-endian), 그 뒤 String
00 03 "num" c1 3930                                                     ← 값 12345 가 c1 = 16비트 정수 인코딩 (문자열 "12345" 대신 2바이트)
10 01 "h" 0d 0d000000 02 00 81 66 02 81 76 02 ff                       ← 타입 0x10 (Hash/HASH_LISTPACK), 키 "h", listpack(f, v)
ff                                                                      ← EOF
ff5f 8b55 5b25 f699                                                     ← CRC64 (8바이트)
```

`redis-check-rdb` 가 같은 구조를 해석한다 ✅:
```
[offset 26] AUX FIELD redis-ver = '7.2.6' ... [offset 81] Selecting DB ID 0
[offset 171] Checksum OK     [info] 5 keys read / 1 expires / 0 already expired
```

### 13-3. 핵심 포인트

| 포인트 | 설명 |
|---|---|
| **길이 인코딩** | 상위 2비트로 6/14/32비트 길이 또는 **특수 인코딩**(정수 `c0/c1/c2`, LZF 압축 `c3`) 구분. **숫자 문자열은 정수로 압축 저장**(`"12345"` → 2바이트) ✅ |
| **값은 메모리 인코딩과 거의 동일** | List/Hash 는 **listpack 바이트열 그대로** 덤프(✅ `0d 0d000000 02 00 81 61...`) → **로드가 빠르다**(파싱/재구성 최소). 메모리 구조 변경 시 RDB 버전도 올라감(`REDIS0011`) |
| **TTL** | `0xFC` + **절대 만료 시각(ms)**. 로드 시 이미 지난 키는 건너뜀(마스터) — Q2 |
| **압축(`rdbcompression yes`)** ✅ | 반복되는 10만 바이트 `a` 값: **압축 안 함 100,181 B → 압축 1,327 B**(LZF). CPU 와 맞교환 |
| **체크섬(`rdbchecksum yes`)** | 파일 끝 CRC64. 끄면 저장/로드 약간 빠르나 **손상 검증 불가** |
| **AUX 필드** | 메타(버전, 시각, 메모리, `aof-base`). 복제 시 레플리카가 RDB 를 받을 때도 같은 포맷 |
| **AOF 와의 관계** | 7.0 Multi Part AOF 의 **base 파일이 RDB 포맷(`appendonly.aof.2.base.rdb`, 헤더 `REDIS0011`)** ✅ (Q2 에서 `.base.rdb` 확인) — "RDB preamble" |
| **저장 방식** | 자식 프로세스가 **임시 파일에 쓰고 완료 후 `rename`** → 저장 중 크래시에도 이전 `dump.rdb` 보존(7장의 안전성) |
| **복제 full sync** | 같은 RDB 를 **디스크 경유 또는 소켓(diskless)** 으로 전송 → 레플리카가 로드 |
| **재시작 후** ✅ | `dbsize`/TTL(절대 시각) 복원, **스크립트 캐시는 사라짐**(Q12) |
| **검증 도구** | `redis-check-rdb file`, `redis-check-aof`(AOF), `redis-cli --rdb`(원격에서 RDB 받기) |

---

## Q14. 클러스터 gossip 메시지 — 얼마나, 어떻게 오가나

10장에서 "gossip 으로 상태를 퍼뜨린다 / 노드가 늘면 O(N²)" 를 문서 기준으로 정리했다. 여기서는 **실제 메시지 양을 측정**한다.

### 14-1. 메시지 종류와 통계 ✅ (`CLUSTER INFO`)

`CLUSTER INFO` 의 `cluster_stats_messages_*` 로 **종류별 송수신 횟수**를 볼 수 있다.

```
cluster_stats_messages_ping_sent:63   pong_sent:61   (meet_sent)
cluster_stats_messages_ping_received:56   pong_received:63   meet_received:5
```
(종류: `ping`, `pong`, `meet`, `fail`, `publish`, `fail-auth-request`, `fail-auth-ack`, `update`, `mfstart` 등 — `fail`/`auth` 는 3부 failover 때 증가)

| 메시지 | 역할 |
|---|---|
| **PING / PONG** | 하트비트 + **gossip 섹션**(다른 노드 몇 개의 상태/플래그/주소) + 슬롯 비트맵 + epoch |
| **MEET** | `CLUSTER MEET` 로 새 노드를 알림(핸드셰이크 시작) |
| **FAIL** | 노드의 FAIL 확정을 **전체에 broadcast** |
| **FAILOVER_AUTH_REQUEST / ACK** | 레플리카 선출 투표 |
| **UPDATE** | 오래된 슬롯 정보를 가진 노드에 최신 정보 전달 |
| **PUBLISH** | 일반 Pub/Sub 을 노드 간 broadcast(6장 증폭 문제) |
| **MFSTART** | 수동 failover 시작 |

### 14-2. 실측 ✅ — 노드 수에 따른 메시지량 (`cluster-node-timeout 3000`, 유휴 상태 10초 평균)

| 노드 수 | **노드당** ping+pong+meet 전송 | **클러스터 전체** |
|---|---|---|
| **6** (마스터 3 + 레플리카 3) | **약 6 /초** | **약 38 /초** |
| **12** (마스터 6 + 레플리카 6) | **약 12 /초** | **약 150 /초** |

- **노드 수 2배(6→12) → 노드당 메시지 약 2배, 전체 약 3.9배(≈4배)** ✅ → **전체 메시지는 O(N²)**(노드마다 모든 노드와 주기적으로 연결/통신), **노드당은 O(N)** 이라는 문서 설명과 일치.
- 이 수치는 **유휴 상태**다. **failover/리샤딩/노드 추가 시** `fail`/`update`/`auth` 메시지로 일시 증가.
- **노드가 수백 개를 넘으면** gossip 트래픽·CPU 가 커져 **`cluster-node-timeout` 을 늘려야 하는 이유** (10장 10-6).

### 14-3. 링크와 노드 정보 ✅

- **노드 간 링크는 2개**(`CLUSTER LINKS`): **`direction to`**(내가 상대에게 거는 링크, 내가 PING 송신) / **`direction from`**(상대가 나에게 거는 링크, 상대 PING 수신). 필드: `create-time`, `events`, `send-buffer-allocated`, `send-buffer-used`(송신 큐 적체 — 느린 노드 탐지, `cluster-link-sendbuf-limit`).
- **`CLUSTER NODES`** 의 `ping-sent`(보낸 PING 중 응답 대기 시작 시각, 0 이면 대기 없음), `pong-recv`(마지막 PONG 수신 시각 ms), `config-epoch`, 링크 상태(`connected`) ✅ — **`ping-sent` 가 0 이 아니고 오래되면 PFAIL 후보**(3부).
- 각 PING 에는 **무작위 일부 노드의 상태**(gossip)가 실려 **직접 연결하지 않은 노드의 정보도 간접 전파**된다(문서 기준: 노드 수의 약 1/10, 최소 3개).

### 14-4. 운영 시사점

| 포인트 | 설명 |
|---|---|
| **용량 계획** | 노드 수 늘릴 때 **마스터만이 아니라 레플리카까지 포함한 전체 노드 수**가 gossip 비용에 영향(12 노드 = 150 msg/s 수준 — 작지만 N²) |
| **방화벽** | 모든 노드 쌍이 버스 포트(+10000)로 통신 가능해야 함(10장) |
| **오탐 방지** | 느린 노드(GC/fork)는 **PONG 지연 → PFAIL**. `node-timeout` 과 gossip 주기(`node-timeout/2` 이내 PING 보장)의 관계 이해 |
| **모니터링** | `cluster_stats_messages_*` 증가율, `CLUSTER LINKS` 의 `send-buffer-used`, `cluster_known_nodes`, `cluster_state` |

---

### ⚠️ 확인하지 못한 부분 (정직 기록)

**로컬(Redis 7.2.6, macOS)에서 직접 확인한 것**: 스레드 수(4 / io-threads 4 에서 7), `lazyfreed_objects` 증가, `redis-rdb-bgsave` 자식 프로세스, Active 만료 OFF 상태에서 Lazy 만 동작하는 모습(`DBSIZE` 유지, `expired_keys` 접근 시 증가), 26개 명령의 Lazy 삭제 여부, Active 만료 속도(`hz` 10/100)와 관련 `INFO` 지표, `expired` 이벤트 발행 시점, replica 의 만료 처리, AOF 의 `PXAT`/`PEXPIREAT`/`DEL` 기록, `active-expire-effort`/`lazyfree-lazy-expire`/`hz`/`dynamic-hz` 기본값.

**문서/소스 지식 기준이며 재현하지 않은 것**:
- **스레드 이름**(`bio_close_file`, `bio_aof`, `bio_lazy_free`, `io_thd_N`): macOS 에서 확인 불가. bio 스레드 종류·개수가 **다른 버전**에서 다를 수 있음
- **이벤트 루프 의사코드와 `beforeSleep` 세부 항목**, 요청 처리 단계 이름(함수명)과 순서는 소스 구조 기억 기준
- **Active 만료 알고리즘 상수**(샘플 20개, 만료 비율 10%, CPU 25%, fast cycle 시간 제한)와 `expires` 테이블이 "key → 절대 시각(ms)" 이라는 구조는 문서/소스 기억 기준 (동작 결과로 간접 확인)
- **RDB 로드 시 마스터는 만료 키 스킵 / replica 는 로드**, 승격 직후 만료 처리 스파이크, **이미 지난 시각 `EXPIREAT` 즉시 삭제**
- **Redis 8 의 I/O 스레딩 개선**, 클러스터/센티널에서의 만료 동작 차이
- 만료 이벤트 실험의 `notify-keyspace-events` 는 `Ex` 로 지정했고 `CONFIG GET` 은 `xE` 로 정규화되어 표시됨(동일 의미)
- **측정 환경**: macOS 단일 호스트, 단일 시도 위주. `hz 10/100` 정리 시간(3.7s/2.6s)은 **상대 비교용**이며 CPU/데이터 크기/동시 부하에 따라 달라짐

**Q3~Q14 (이번 추가분) — 로컬(7.2.6, macOS)에서 직접 확인한 것**: libc allocator 에서 키 90% 삭제 후 `used_memory` vs RSS 변화, `MEMORY PURGE`/`activedefrag` 제약; `DEBUG HTSTATS` 로 점진적 rehash(요청마다 이전, `activerehashing no` 에서 정지, cron 진행, 수축); `OBJECT ENCODING` 임계값 전환(String/Hash/List/Set/ZSet)과 메모리·속도 비교; 샘플 수별 근사 LRU 정확도, LFU 카운터 증가·포화; MULTI/EXEC/WATCH 10개 시나리오; 블로킹 명령 8개 시나리오; `replid`/`offset`/`PING` 전파/부분·전체 재동기화 4개 시나리오; Stream radix tree 노드 증가와 consumer group/PEL 명령; RESP2/RESP3 소켓 응답; Lua 캐시·에러·AOF effects 복제·Function; RDB 171바이트 hexdump 해석과 `redis-check-rdb`; 6/12 노드 gossip 메시지량.

**Q3~Q14 — 문서/소스 지식 기준이며 재현하지 않은 것**:
- **Q3**: **jemalloc** 의 반환·단편화 동작과 `MEMORY PURGE`/`activedefrag` 실제 효과(이 빌드는 libc), `allocator_frag_ratio`/`rss_overhead_ratio` 해석, Linux THP 영향
- **Q4**: dict **수축 임계(약 1/8)**, fork 중 rehash 억제와 강제 확장 비율(약 4:1), 확장 중 메모리 1.5배, `SCAN` 과 rehash 의 상호작용
- **Q5**: **`MEMORY USAGE` 의 절대 바이트**(libc 반올림 포함, 상대 비교용). 특히 listpack 크기가 필드 수가 달라도 같게 나온 경우가 있어(예: 128/129 필드 모두 1,592 B) **정확한 값으로 쓰지 말 것**. `list-max-listpack-size` 의 다른 값(양수 = 개수 기준) 동작, 7.2 에서 quicklist → listpack 되돌림 조건
- **Q6**: LRU 의 24비트 시계 해상도와 eviction pool 세부, LFU 상위 16비트 감소 시각 구조와 감소 계산식(`lfu-decay-time`), 실제 eviction 루프(`performEvictions`) 구현. 샘플링 실험은 **작은 값(200B)·2만 키·단일 시도**(무작위 샘플이라 결과가 실행마다 다름)
- **Q7**: `watched_keys`/`DIRTY_CAS`/`signalModifiedKey` 등 **내부 이름과 구조**, 롤백 미지원의 설계 철학(공식 문서 설명), 클러스터에서의 트랜잭션
- **Q8**: `blocking_keys`/`ready_keys`/`handleClientsBlockedOnKeys` 등 내부 이름, 블로킹 소비자 다수의 한 배치 처리 영향, `WAITAOF`
- **Q9**: `REPLCONF ACK` 1초 주기, replid2 허용 조건의 정확한 식(`second_repl_offset` 비교), 7.0+ 공유 복제 버퍼의 블록 단위(backlog histlen 이 설정의 2배로 보인 이유 — **관찰만 했고 정확한 원인은 미확인**), backlog 산정 공식은 일반 가이드
- **Q10**: radix tree 키 = 첫 entry ID, 노드 listpack 의 master entry·플래그 구조, `stream-node-max-*` 외의 노드 분할 규칙(실측은 100개 단위만 확인), `ms-*` 형식 ID, 시계 역행 시 처리
- **Q11**: RESP3 의 `#`/`=`/`(` 타입과 Push 상세, 서버 내부 reply 경로(`addReply`, `handleClientsWithPendingWrites`, EPOLLOUT)의 함수명, **프로토콜 오류 후 연결 종료**, 인라인 요청 길이 제한(64KB)은 실험 설계가 부정확해 **제외**
- **Q12**: 스크립트 **쓰기 후 `SCRIPT KILL` 불가(`UNKILLABLE`)**, Lua 샌드박스 내부, `allow-oom` 등 플래그 의미, 7.0 에서 스크립트 복제 방식이 effects 로 일원화된 시점은 문서 기준
- **Q13**: **체크섬 변조 후 로드 실패 동작**은 시도했으나 **변조 위치가 의도와 달라 유효한 검증이 되지 못해 제외**. 타입 코드(`0x00`/`0x10`/`0x12`)의 이름(`RDB_TYPE_*`)과 길이 인코딩 상위 2비트 규칙, LZF(`c3`) 구조는 문서 기준
- **Q14**: gossip 에 실리는 노드 수(약 1/10, 최소 3), `node-timeout/2` PING 보장, 각 메시지 종류의 페이로드 구조(패킷 수준 검증 안 함), **유휴 상태만 측정**(failover/리샤딩 중 메시지량 미측정). 6/12 노드는 **단일 호스트 loopback** 측정이라 절대값은 상대 비교용
