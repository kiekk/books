### CHAPTER 3 레디스 기본 개념

레디스가 **왜 Data Structure Server**인지 보여주는 장. 각 자료 구조의 **용도·주요 명령어·시간복잡도**를 정리하고, **왜 List는 인덱스 접근이 O(N)이고 Sorted Set은 O(log N)인가** 같은 **내부 구조 기반의 이유**까지 짚는다. 운영 중 피해야 할 명령어(`KEYS`, `DEL` on 대형 키)도 함께.

#### 목차

- [1부. 레디스의 7가지 핵심 자료 구조](#1부-레디스의-7가지-핵심-자료-구조)
- [2부. String](#2부-string)
- [3부. List](#3부-list)
- [4부. Hash](#4부-hash)
- [5부. Set](#5부-set)
- [6부. Sorted Set](#6부-sorted-set)
- [7부. 그 외 — Bitmap / HyperLogLog / Geo / Stream](#7부-그-외--bitmap--hyperloglog--geo--stream)
- [8부. 왜 List 인덱스는 O(N), Sorted Set은 O(log N)?](#8부-왜-list-인덱스는-on-sorted-set은-olog-n)
- [9부. 키 관리 명령](#9부-키-관리-명령)
- [10부. KEYS, DEL의 문제점과 대체 명령](#10부-keys-del의-문제점과-대체-명령)

---

## 1부. 레디스의 7가지 핵심 자료 구조

| 자료 구조 | 용도 | 대표 사용처 |
|---|---|---|
| **String** | 바이너리 안전 문자열 (최대 512 MB) | 캐시, 카운터, 세션 |
| **List** | 양방향 Linked List | 큐, 최근 N개 리스트 |
| **Hash** | 필드-값 쌍 | 객체 저장 (user:123 → {name, age}) |
| **Set** | 중복 없는 멤버 | 태그, 유니크 집합 |
| **Sorted Set** | 점수 기반 정렬된 Set | 리더보드, 랭킹 |
| **Bitmap** | 비트 단위 조작 | 활성 사용자, 플래그 |
| **HyperLogLog** | 유니크 카운팅 (근사) | 유니크 방문자 수 |
| **Geo** | 지리 좌표 | 주변 매장 검색 |
| **Stream** | 로그/이벤트 스트림 | 메시지 큐, 이벤트 소싱 |

### 자료 구조별 내부 인코딩 (중요)

레디스는 **크기·조건에 따라 내부 인코딩을 자동 전환** — 메모리 효율 ↔ 성능 트레이드오프:

| 자료 구조 | 작을 때 | 커지면 |
|---|---|---|
| **String** | int / embstr | raw |
| **List** | listpack | quicklist |
| **Hash** | listpack | hashtable |
| **Set** | intset / listpack | hashtable |
| **Sorted Set** | listpack | **skiplist + hashtable** |
| **Stream** | listpack 노드 (radix tree) | — |

→ `OBJECT ENCODING key`로 확인 가능. 2장의 임계값(hash-max-listpack-entries 등)으로 전환 조절.

---

## 2부. String

### 주요 명령어

| 명령 | 복잡도 | 설명 |
|---|---|---|
| `SET key value [EX sec] [PX ms] [NX\|XX]` | O(1) | 저장. **NX**(없을 때만), **XX**(있을 때만), **EX**(초 TTL), **PX**(ms TTL) |
| `GET key` | O(1) | 조회 |
| `MSET k1 v1 k2 v2` | O(N) | 다중 저장 |
| `MGET k1 k2` | O(N) | 다중 조회 |
| `INCR key` | O(1) | 정수 1 증가 |
| `DECR key` | O(1) | 정수 1 감소 |
| `INCRBY key N` | O(1) | 정수 N 증가 |
| `INCRBYFLOAT key N` | O(1) | 실수 증가 |
| `APPEND key value` | O(1)* | 끝에 추가 (분할상환) |
| `STRLEN key` | O(1) | 길이 |
| `GETRANGE key start end` | O(N) | 부분 조회 |
| `SETRANGE key offset value` | O(1)* | 부분 쓰기 |

### 옵션 활용 패턴

**분산 락**:
```
SET lock:order:123 "worker-1" NX EX 10
# NX = 없을 때만, EX 10초 TTL
```

**카운터**:
```
INCR article:1234:views
INCRBY article:1234:views 10
```

**TTL 설정**:
```
SET session:abc "data" EX 1800     # 30분 후 만료
```

---

## 3부. List

Linked List 기반. **양 끝에서의 삽입/삭제가 O(1)**, 가운데 접근은 O(N).

### 주요 명령어

| 명령 | 복잡도 | 설명 |
|---|---|---|
| `LPUSH key v1 v2` | O(1) | 왼쪽에 추가 |
| `RPUSH key v1 v2` | O(1) | 오른쪽에 추가 |
| `LPOP key [count]` | O(N) (count 제거) | 왼쪽에서 꺼냄 |
| `RPOP key [count]` | O(N) | 오른쪽에서 꺼냄 |
| `BLPOP key timeout` | O(1) | **블로킹 Pop** (큐 비면 대기) |
| `BRPOP key timeout` | O(1) | 블로킹 (오른쪽) |
| `LRANGE key start stop` | O(S+N) | 범위 조회 |
| `LLEN key` | O(1) | 길이 |
| `LINDEX key index` | **O(N)** | **인덱스 조회 — 느림** |
| `LSET key index value` | O(N) | 인덱스 쓰기 |
| `LINSERT key BEFORE\|AFTER pivot value` | O(N) | pivot 기준 삽입 |
| `LREM key count value` | O(N) | 값 제거 |
| `LTRIM key start stop` | O(N) | 범위만 남기고 자름 |

### 주요 사용 패턴

**단순 큐 (FIFO)**:
```
LPUSH queue "task1"
BRPOP queue 0        # 블로킹, 작업 올 때까지 대기
```

**최근 N개 저장**:
```
LPUSH recent:user:123 "action1"
LTRIM recent:user:123 0 99    # 상위 100개만 유지
```

**스택 (LIFO)**:
```
LPUSH stack "item"
LPOP stack
```

---

## 4부. Hash

**필드-값** 쌍. 객체 저장에 적합. 작으면 listpack(메모리 효율), 커지면 hashtable.

### 주요 명령어

| 명령 | 복잡도 | 설명 |
|---|---|---|
| `HSET key field value [f v...]` | O(1) per field | 필드 저장 (여러 개도 가능) |
| `HGET key field` | O(1) | 필드 조회 |
| `HMGET key f1 f2` | O(N) | 다중 필드 |
| `HGETALL key` | **O(N)** | 전체 조회 — **큰 Hash면 위험** |
| `HKEYS key` | O(N) | 모든 필드 |
| `HVALS key` | O(N) | 모든 값 |
| `HDEL key field` | O(N) | 필드 삭제 |
| `HEXISTS key field` | O(1) | 필드 존재 여부 |
| `HLEN key` | O(1) | 필드 수 |
| `HINCRBY key field N` | O(1) | 필드 정수 증가 |
| `HSCAN key cursor` | O(1) per call | **커서 기반 순회** (큰 Hash용) |

### 사용 패턴

**객체 저장**:
```
HSET user:1234 name "Alice" age 30 email "a@b.com"
HGET user:1234 email
HINCRBY user:1234 points 10
```

**장단점 vs String으로 각각 저장**:
- Hash: `user:1234` 하나 → 전체 조회/삭제 간단, **메모리 효율 ↑** (listpack일 때)
- String × N: `user:1234:name`, `user:1234:age` → 필드별 TTL 가능하지만 메모리 비효율

→ **같은 객체의 여러 속성은 Hash**가 정답.

---

## 5부. Set

**중복 없는 멤버**의 집합. 순서 없음.

### 주요 명령어

| 명령 | 복잡도 | 설명 |
|---|---|---|
| `SADD key m1 m2` | O(N) | 멤버 추가 |
| `SREM key m1 m2` | O(N) | 제거 |
| `SMEMBERS key` | **O(N)** | 전체 조회 — **큰 Set 위험** |
| `SISMEMBER key m` | O(1) | 존재 확인 |
| `SMISMEMBER key m1 m2` | O(N) | 다중 존재 확인 (Redis 6.2+) |
| `SCARD key` | O(1) | 멤버 수 |
| `SRANDMEMBER key [count]` | O(1) / O(count) | 랜덤 멤버 |
| `SPOP key [count]` | O(1) / O(count) | 랜덤 pop |
| `SINTER k1 k2` | O(N*M) | **교집합** |
| `SUNION k1 k2` | O(N) | 합집합 |
| `SDIFF k1 k2` | O(N) | 차집합 |
| `SINTERSTORE dest k1 k2` | O(N*M) | 교집합 결과 저장 |
| `SSCAN key cursor` | O(1) per call | 커서 순회 |

### 사용 패턴

**태그**:
```
SADD article:1234:tags "redis" "nosql" "cache"
SISMEMBER article:1234:tags "redis"
```

**공통 친구 찾기**:
```
SADD user:A:friends "B" "C" "D"
SADD user:E:friends "C" "D" "F"
SINTER user:A:friends user:E:friends    # → {"C", "D"}
```

---

## 6부. Sorted Set

**점수(score) 기반 정렬된 Set**. 각 멤버에 **double 점수** 할당 → **점수 순서로 자동 정렬**.

### 주요 명령어

| 명령 | 복잡도 | 설명 |
|---|---|---|
| `ZADD key [NX\|XX\|GT\|LT] [CH] [INCR] score member` | **O(log N)** | 추가 |
| `ZREM key m1 m2` | O(M log N) | 제거 |
| `ZSCORE key member` | O(1) | 점수 조회 |
| `ZINCRBY key N member` | O(log N) | 점수 증가 |
| `ZRANK key member` | **O(log N)** | 순위 (낮은 점수부터) |
| `ZREVRANK key member` | O(log N) | 순위 (높은 점수부터) |
| `ZRANGE key start stop [REV] [WITHSCORES]` | O(log N + M) | 순위 범위 조회 |
| `ZRANGEBYSCORE key min max` | O(log N + M) | 점수 범위 조회 |
| `ZRANGEBYLEX key min max` | O(log N + M) | 사전 순 범위 (점수 동일 가정) |
| `ZCARD key` | O(1) | 멤버 수 |
| `ZCOUNT key min max` | O(log N) | 범위 내 멤버 수 |
| `ZPOPMIN key [count]` | O(log N) | 최저 점수 pop |
| `ZPOPMAX key [count]` | O(log N) | 최고 점수 pop |

### 사용 패턴

**리더보드**:
```
ZADD leaderboard 2500 "alice"
ZADD leaderboard 1800 "bob"
ZADD leaderboard 3200 "charlie"

# 상위 10명
ZREVRANGE leaderboard 0 9 WITHSCORES

# alice의 랭크
ZREVRANK leaderboard "alice"

# 점수 범위 조회 (2000~3000)
ZRANGEBYSCORE leaderboard 2000 3000
```

**시간 기반 큐 (지연 처리)**:
```
# 처리 시각을 score로
ZADD delayed_tasks 1697000000 "task1"
# 현재 시각 이하 작업 꺼내기
ZRANGEBYSCORE delayed_tasks -inf <now>
```

**ZADD 옵션**:
- **NX** — 멤버 없을 때만 추가
- **XX** — 멤버 있을 때만 업데이트
- **GT** — 기존 점수보다 **큰 경우만** 업데이트
- **LT** — 기존 점수보다 **작은 경우만**
- **CH** — 변경된 수 반환
- **INCR** — 점수 증가 (`ZINCRBY`와 동등)

---

## 7부. 그 외 — Bitmap / HyperLogLog / Geo / Stream

### Bitmap

String 위에 **비트 단위** 조작.

```
SETBIT user:active:2026-10-11 123 1   # 사용자 123 활성
GETBIT user:active:2026-10-11 123
BITCOUNT user:active:2026-10-11       # 전체 활성 수
BITOP AND dest key1 key2              # 비트 연산
```

**사용**: DAU 집계 (사용자 1명 = 1비트, 1000만 명 = 1.25 MB)

### HyperLogLog

**근사 유니크 카운팅** (오차 ~0.81%). **12 KB로 수억 개 유니크 추정**.

```
PFADD unique:visitors "user1" "user2"
PFCOUNT unique:visitors
PFMERGE merged daily:1 daily:2       # 여러 HLL 병합
```

**사용**: UV(Unique Visitor), 유니크 검색어 수.

### Geo

지리 좌표(longitude, latitude) 저장 + 거리 검색. 내부는 **Sorted Set의 geohash 점수**로 구현.

```
GEOADD stores 127.0276 37.4979 "강남점"
GEOADD stores 127.0286 37.4988 "역삼점"
GEOSEARCH stores FROMLONLAT 127.0276 37.4979 BYRADIUS 500 m
GEODIST stores "강남점" "역삼점" m
```

### Stream (Redis 5.0+)

**로그/이벤트 스트림**. Kafka 유사. 자세한 내용은 6장에서.

```
XADD mystream * event "signup" user "alice"
XLEN mystream
XRANGE mystream - +
XREAD COUNT 10 STREAMS mystream 0
XGROUP CREATE mystream grp1 $ MKSTREAM
XREADGROUP GROUP grp1 consumer1 COUNT 10 STREAMS mystream >
```

---

## 8부. 왜 List 인덱스는 O(N), Sorted Set은 O(log N)?

**가장 많이 받는 질문**. 둘 다 "정렬된 데이터 구조처럼 보이는데" 왜 인덱스 접근 성능이 다른가.

### List 내부 구조 — **quicklist**

![list 구조](https://redis.io/images/)

```
Head ────[Node1: listpack]────[Node2: listpack]────[Node3: listpack]────Tail
         (10개 원소)         (10개 원소)         (10개 원소)
```

- 각 노드가 **listpack**(연속 메모리에 원소 압축 저장)을 품은 **이중 연결 리스트**
- Redis 3.2 이전은 순수 linked list, 이후부터 quicklist
- **메모리 효율 ↑** (각 노드가 수십 개 원소 묶음)

**LINDEX i(인덱스 접근) 동작**:

```python
# 의사 코드
def LINDEX(list, index):
    node = list.head
    offset = index
    while node and offset >= node.count:
        offset -= node.count
        node = node.next           # ← 노드를 하나씩 순회
    return node.listpack[offset]
```

- `index`에 도달하려면 **노드를 선형으로 순회**
- **최악 O(N/노드크기)** ≈ **O(N)**
- Head/Tail은 O(1) (양 끝 포인터 유지)

**왜 포인터 배열로 안 만들었나?**
- Redis는 **메모리 효율 최우선**
- 랜덤 접근용 배열(array) 구조는 **포인터 N개 저장** 필요 → 메모리 ↑
- **"리스트는 큐로 쓰라"**는 설계 철학 — 양 끝 접근만 최적화

### Sorted Set 내부 구조 — **Skip List + Hash Table**

```
Hash Table (member → score 매핑)
┌──────────┬──────────┐
│ "alice"  │  2500    │
│ "bob"    │  1800    │
│ "charlie"│  3200    │
└──────────┴──────────┘

Skip List (점수 기준 정렬)

Level 3:  ──────────────────────── charlie(3200)
Level 2:  ──── alice(2500) ──── charlie(3200)
Level 1:  bob(1800) ── alice(2500) ── charlie(3200)
Level 0:  bob(1800) → alice(2500) → charlie(3200)
```

- **Hash Table**: `member → score` 조회 **O(1)** (ZSCORE)
- **Skip List**: 점수 기준 **정렬 유지**, 범위/순위 조회 **O(log N)**

### Skip List란

> **확률적 자료 구조**. 다층 레벨의 포인터로 **O(log N) 랜덤 접근**을 지원.

**직관**: 가장 아래층(Level 0)은 모든 원소를 연결한 리스트. 윗 레벨은 **절반씩 건너뛰는** 포인터. 조회 시 **윗 레벨부터 내려오며 빠르게 범위 좁힘** → **O(log N)**.

이진 탐색 트리(BST)와 유사한 성능이지만 **구현이 훨씬 단순** + **락 없이 동시성 처리 쉬움** → Redis가 채택.

**ZRANGE / ZRANK 동작**:
- 최상위 레벨에서 시작 → 포인터 따라 점프 → 레벨 내려가며 좁힘
- **O(log N)**에 원하는 위치 도달
- 이후 M개 원소는 Level 0을 선형 순회 → **O(log N + M)**

### 핵심 차이 요약

| 축 | List | Sorted Set |
|---|---|---|
| **내부 구조** | quicklist (listpack 노드의 이중 연결 리스트) | **Skip List + Hash Table** |
| **인덱스/순위 접근** | **O(N)** (노드 순회) | **O(log N)** (레벨 점프) |
| **양 끝 접근** | O(1) | ZPOPMIN/MAX O(log N) |
| **범위 조회** | O(S+N) | O(log N + M) |
| **메모리** | 매우 효율적 | 각 원소당 skip list 노드 + hash 엔트리 → 많음 |
| **용도** | 큐, 최근 N개 | 순위, 랭킹, 점수 기반 조회 |

### 왜 두 구조 다른가 — 설계 철학

- **List**: "앞뒤로 넣고 빼는" 큐 패턴 가정 → **메모리 효율 극대화** + 랜덤 접근은 희생
- **Sorted Set**: "순위·범위 조회 필수" 가정 → **메모리를 더 쓰더라도** O(log N) 보장

→ **데이터에 맞는 구조 선택이 성능의 90%**. "리스트 가운데 자주 접근해야 함" → **Sorted Set으로 전환**.

---

## 9부. 키 관리 명령

### TTL 관련

| 명령 | 복잡도 | 설명 |
|---|---|---|
| `EXPIRE key sec` | O(1) | 초 단위 TTL |
| `PEXPIRE key ms` | O(1) | 밀리초 TTL |
| `EXPIREAT key timestamp` | O(1) | 특정 시각까지 |
| `TTL key` | O(1) | 남은 TTL(초). -1=영구, -2=없음 |
| `PTTL key` | O(1) | ms 단위 |
| `PERSIST key` | O(1) | TTL 제거 (영구화) |

### 키 조작

| 명령 | 복잡도 | 설명 |
|---|---|---|
| `EXISTS key [...]` | O(N) | 존재 확인 |
| `TYPE key` | O(1) | 자료 구조 종류 |
| `RENAME key newkey` | O(1) | 리네임 |
| `RENAMENX key newkey` | O(1) | newkey 없을 때만 |
| `COPY src dst [DB] [REPLACE]` | O(N) | 복사 (Redis 6.2+) |
| `OBJECT ENCODING key` | O(1) | 내부 인코딩 확인 |
| `DEBUG OBJECT key` | O(1) | 디버그 정보 |
| `MEMORY USAGE key` | O(N) | 메모리 사용량 |

### DB 선택

```
SELECT 1                 # DB 1로 전환
MOVE key 2               # key를 DB 2로 이동
```

- 클러스터 모드는 DB 0만 사용 가능
- 멀티 DB는 **네임스페이스 분리용** — 보안·격리 보장 X

---

## 10부. KEYS, DEL의 문제점과 대체 명령

### KEYS 명령의 문제

```
KEYS pattern*       # 패턴 매칭되는 모든 키 조회
```

**문제**:
- **O(N)** — 전체 키 공간을 **순차 스캔**
- **단일 스레드 블록** — N이 수백만이면 **수 초 이상 블록**
- 그 동안 **다른 모든 명령 대기** → 전체 서비스 영향
- **Production에서 금지**

### 대체 — SCAN

```
SCAN cursor [MATCH pattern] [COUNT hint] [TYPE string]
```

**특징**:
- **커서 기반 반복** (여러 번 호출)
- 매 호출마다 **일부만 반환** + 다음 커서 반환
- 커서=0이면 완료
- 단일 호출은 **O(1)에 가까움** (COUNT만큼만 스캔)
- **메인 스레드 블록 없음**

**사용 예 (Python redis-py)**:
```python
cursor = 0
while True:
    cursor, keys = r.scan(cursor, match="user:*", count=100)
    for key in keys:
        process(key)
    if cursor == 0:
        break
```

**또는 iter 헬퍼**:
```python
for key in r.scan_iter(match="user:*", count=100):
    process(key)
```

**주의점**:
- **COUNT는 "힌트"일 뿐** 정확한 반환 수 아님
- **중복 반환 가능성** (해시 리사이즈 중)
- **스냅샷 아님** — 진행 중 추가된 키는 못 볼 수 있음

**SCAN 종류**:
- `SCAN` — 전체 키 공간
- `HSCAN key cursor` — Hash의 필드
- `SSCAN key cursor` — Set의 멤버
- `ZSCAN key cursor` — Sorted Set의 멤버

### DEL 명령의 문제

```
DEL key
```

**단일 키 삭제는 보통 O(1)이지만**, **큰 자료 구조**(수백만 원소 Hash/Set/ZSet)는:

- 원소 수에 비례해 **O(N)**
- 모든 메모리 해제가 **메인 스레드에서** 발생
- 수 초 이상 블록 가능

### 대체 — UNLINK

```
UNLINK key [key2 key3 ...]
```

**특징**:
- 명령 자체는 **O(1)** — 키를 **키 공간에서 즉시 분리**(unlink)
- **실제 메모리 해제는 백그라운드 스레드**에서 수행 (Lazy Free)
- **Redis 4.0+**

**동작 원리**:

```
UNLINK bigkey
  ↓
메인 스레드: 키 공간에서 bigkey 포인터 제거 (O(1))
             → 사용자에게 OK 즉시 응답
             → 자료 구조는 백그라운드 작업 큐에 등록

백그라운드 스레드: 큐에서 꺼내서 실제 free() 수행
                   (오래 걸리든 메인 스레드와 무관)
```

**결론**: **큰 키 삭제는 무조건 UNLINK**.

### FLUSHALL / FLUSHDB도 마찬가지

```
FLUSHALL ASYNC         # 비동기 플러시
FLUSHDB ASYNC
```

- `ASYNC` 플래그로 백그라운드 수행
- 또는 2장의 `lazyfree-lazy-user-flush yes` 설정

### 종합 비교표

| 작업 | 위험 명령 | 대체 명령 | 이유 |
|---|---|---|---|
| 패턴 매칭 키 찾기 | `KEYS pattern*` | **`SCAN 0 MATCH pattern*`** | 블로킹 vs 커서 기반 |
| 큰 Hash 필드 순회 | `HGETALL bigkey` | **`HSCAN bigkey 0`** | O(N) 블록 vs 커서 |
| 큰 Set 조회 | `SMEMBERS bigset` | **`SSCAN bigset 0`** | 동일 |
| 큰 ZSet 조회 | — | **`ZSCAN zbigset 0`** | — |
| 키 삭제 (큰 자료 구조) | `DEL bigkey` | **`UNLINK bigkey`** | 메인 블록 vs 백그라운드 |
| DB 비우기 | `FLUSHALL` | **`FLUSHALL ASYNC`** | 동일 |

### 추가 — 위험 명령 운영 차단

2장 참고. 운영 환경에서는 **아예 rename 처리**:

```
# redis.conf
rename-command KEYS ""
rename-command FLUSHALL ""
rename-command FLUSHDB ""
rename-command CONFIG ""
rename-command DEBUG ""
```

→ 실수나 공격자가 호출해도 **"unknown command"** 응답.

---

### 요약

- 레디스는 **Data Structure Server** — String/List/Hash/Set/Sorted Set + Bitmap/HLL/Geo/Stream
- 각 자료 구조는 **내부 인코딩 자동 전환** — 작으면 메모리 효율(listpack/intset), 크면 성능 보장(hashtable/skiplist)
- **ZADD 옵션** (NX/XX/GT/LT/CH/INCR), **SET 옵션** (NX/XX/EX/PX)으로 **원자적 조건 쓰기** 가능 → 분산 락 등에 활용
- **List 인덱스 O(N) vs Sorted Set O(log N)** 이유:
  - List는 **quicklist(listpack 노드 연결 리스트)** → 노드 선형 순회
  - Sorted Set은 **skip list + hash table** → 레벨 점프로 O(log N) 랜덤 접근
  - 설계 철학: List는 "큐 전용(메모리 효율)", Sorted Set은 "순위 쿼리(성능)"
- **운영 금지 명령**:
  - **`KEYS *`** → `SCAN` (커서 기반)
  - **`HGETALL/SMEMBERS` on 큰 자료 구조** → `HSCAN/SSCAN/ZSCAN`
  - **`DEL` on 큰 자료 구조** → `UNLINK` (Lazy Free)
  - **`FLUSHALL/FLUSHDB`** → `ASYNC` 또는 `lazyfree-lazy-user-flush yes`
- 운영 환경은 `rename-command`로 **아예 차단** 권장
