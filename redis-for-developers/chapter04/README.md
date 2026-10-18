### CHAPTER 4 레디스 자료 구조 활용 사례

3장이 **자료 구조의 사용법**이었다면, 4장은 **실무에서 어떤 문제를 어떤 구조로 푸는가**. 각 패턴 끝에 **사이드 프로젝트 아이디어**를 함께 덧붙임.

#### 목차

- [1부. Sorted Set 활용](#1부-sorted-set-활용)
- [2부. 다양한 카운팅 방법](#2부-다양한-카운팅-방법)
- [3부. HyperLogLog — 유니크 카운팅](#3부-hyperloglog--유니크-카운팅)
- [4부. Geospatial — 위치 기반 서비스](#4부-geospatial--위치-기반-서비스)
- [5부. 그 외 활용 패턴](#5부-그-외-활용-패턴)
- [6부. 사이드 프로젝트 아이디어 모음](#6부-사이드-프로젝트-아이디어-모음)

---

## 1부. Sorted Set 활용

Sorted Set은 **"점수로 정렬된 중복 없는 집합"**. 점수 기반 범위/순위 쿼리가 모두 **O(log N)**이라 **실시간 랭킹** 영역에서 압도적 강점.

### 1.1 실시간 리더보드 (가장 전형)

```
# 유저 점수 등록/업데이트
ZADD leaderboard 2500 "alice"
ZADD leaderboard 1800 "bob"

# 상위 10명
ZREVRANGE leaderboard 0 9 WITHSCORES

# 특정 유저 순위
ZREVRANK leaderboard "alice"       # 0부터 시작

# 자기 ± 5등 보기
rank = ZREVRANK leaderboard "alice"   # 예: 23
ZREVRANGE leaderboard max(0, rank-5) rank+5 WITHSCORES

# 점수 증가
ZINCRBY leaderboard 100 "alice"
```

**실무 포인트**:
- **점수 동률** 시 멤버 **사전순**으로 정렬
- 수백만 유저도 전체 랭킹 O(log N) + O(M)
- 주기적으로 **스냅샷** 떠서 분석 DB에 적재

### 1.2 지연 작업 큐 (Delayed Queue)

```
# 처리할 시각을 score로 저장
ZADD delayed_tasks 1697000000 "send_email:user:123"
ZADD delayed_tasks 1697000060 "charge:order:456"

# 1초마다 polling: 현재 시각 이하 꺼내기
now = unix_timestamp()
tasks = ZRANGEBYSCORE delayed_tasks -inf now LIMIT 0 100
for t in tasks:
    process(t)
    ZREM delayed_tasks t
```

**원자적 pop (경쟁 조건 방지)**:
```
# Lua 스크립트로 pop + delete 묶기
EVAL "
  local now = ARGV[1]
  local tasks = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', now, 'LIMIT', 0, 100)
  if #tasks > 0 then
    redis.call('ZREM', KEYS[1], unpack(tasks))
  end
  return tasks
" 1 delayed_tasks <now>
```

### 1.3 레이트 리밋 (슬라이딩 윈도우)

```
# 요청 올 때마다 timestamp를 member로 저장
now = unix_timestamp_ms()
key = "ratelimit:user:123"
window = 60000       # 1분

ZADD key now now                        # score=member=now
ZREMRANGEBYSCORE key -inf (now-window)  # 윈도우 밖 제거
count = ZCARD key
if count > 100:
    reject()
EXPIRE key 60
```

- **정확한 슬라이딩 윈도우** 레이트 리미팅
- 토큰 버킷/고정 윈도우보다 정밀

### 1.4 최근 조회 상품 (Recently Viewed)

```
# score = 조회 시각 (최근일수록 큼)
ZADD recent:user:123 <timestamp> "product:456"
ZADD recent:user:123 <timestamp> "product:789"

# 최근 20개
ZREVRANGE recent:user:123 0 19

# 오래된 것 자동 정리
ZREMRANGEBYRANK recent:user:123 0 -51    # 상위 50개만 유지
```

### 1.5 자동완성 (Autocomplete)

```
# 모든 멤버 점수 0, lex 순서로 정렬
ZADD autocomplete 0 "redis"
ZADD autocomplete 0 "redis-cluster"
ZADD autocomplete 0 "redis-cli"
ZADD autocomplete 0 "redux"

# "redis" 접두어로 검색
ZRANGEBYLEX autocomplete "[redis" "[redis\xff"
```

---

## 2부. 다양한 카운팅 방법

### 2.1 단순 카운터 — String INCR

```
INCR article:1234:views        # 조회수 +1
INCRBY article:1234:views 5
GET article:1234:views
```

- 가장 가볍고 빠름
- 하지만 **키 폭발 주의** — 글마다 키 생성 (수백만 글 = 수백만 키)

### 2.2 다중 카운터 — Hash HINCRBY

```
# 하나의 Hash에 여러 카운터 묶음
HINCRBY article:1234 views 1
HINCRBY article:1234 likes 1
HINCRBY article:1234 shares 1
HGETALL article:1234
```

- **메모리 효율 ↑** (Hash가 작으면 listpack)
- **원자적 묶음** (한 글의 여러 지표 함께)

### 2.3 시간 단위 카운터 — key에 시각 포함

```
# 분/시/일 단위 집계
INCR views:article:1234:2026-10-11
INCR views:article:1234:2026-10-11-14    # 시간대별
INCR views:article:1234:2026-10-11-14-30 # 분 단위
EXPIRE views:article:1234:2026-10-11 7776000   # 90일 후 자동 소멸
```

- **TTL 설정으로 자동 정리**
- 특정 날짜 집계 쉬움

### 2.4 Bitmap 카운팅 — 불리언 상태

```
# 유저 123이 특정 날 활성
SETBIT active:2026-10-11 123 1

# 특정 날 활성 유저 수 (DAU)
BITCOUNT active:2026-10-11

# 비트 OR로 특정 기간 활성 (WAU/MAU)
BITOP OR active:2026-W41 active:2026-10-07 active:2026-10-08 ... active:2026-10-13
BITCOUNT active:2026-W41
```

- **100만 유저 = 125 KB**로 하루 상태 저장
- **유저 ID가 작은 정수**일 때 효율 극대

### 2.5 Set 카운팅 — 유니크 멤버 수

```
SADD visitors:2026-10-11 "user:123"
SADD visitors:2026-10-11 "user:456"
SCARD visitors:2026-10-11        # 유니크 방문자 수
```

- 정확한 카운트
- **단점: 메모리** — 1000만 유니크 = 수백 MB (멤버 길이 따라)

### 2.6 HyperLogLog — 근사 유니크

(3부에서 상세)

### 카운팅 방법 종합 비교

| 방법 | 정확도 | 메모리 | 사용처 |
|---|---|---|---|
| **INCR** (String) | 정확 | 키마다 ~50B | 단순 수치 |
| **HINCRBY** (Hash) | 정확 | 효율적 | 다중 지표 |
| **Bitmap** | 정확 (bool) | 유저당 1bit | 활성 여부 (DAU) |
| **Set** | 정확 | **큼** | 소규모 유니크 |
| **HyperLogLog** | ~0.81% 오차 | **12 KB 고정** | 대규모 유니크 |

---

## 3부. HyperLogLog — 유니크 카운팅

### 개념

**근사 유니크 카운팅** 알고리즘. 수학적으로 멤버의 **해시 값 비트 패턴**을 분석해 유니크 개수를 추정.

- **12 KB** 고정 메모리로 **최대 2^64 유니크 추정**
- **표준 오차 0.81%**
- 멤버 자체는 저장하지 않음 → **메모리 극한 효율** + **개별 멤버 조회 불가**

### 주요 명령

```
PFADD unique:visitors "user:123" "user:456" "user:789"
PFCOUNT unique:visitors                          # 추정 유니크 수
PFCOUNT unique:visitors:day1 unique:visitors:day2 # 여러 HLL 합집합 카운트
PFMERGE merged unique:visitors:day1 unique:visitors:day2  # 병합 저장
```

### 전형 사용 사례

- **DAU/MAU** (유니크 방문자)
- **유니크 검색어 수**
- **유니크 광고 노출**
- **CDN 요청 유니크 IP**

### Set vs HLL 선택 기준

| 조건 | 선택 |
|---|---|
| **멤버 개별 조회 필요** (알리스가 방문했는가?) | Set / SMEMBERS |
| 유니크 수만 필요, 수천~수만 명 | Set |
| 유니크 수만 필요, **수십만+** | **HyperLogLog** |
| 메모리 예산 극도로 제한 | HLL |
| 0.81% 오차 허용 불가 | Set |

### PFMERGE의 강점

```
# 매일 HLL 하나씩
PFADD hll:2026-10-01 ... 
PFADD hll:2026-10-02 ...

# 한 달 유니크 수 (중복 제거)
PFMERGE hll:2026-10 hll:2026-10-01 hll:2026-10-02 ... hll:2026-10-31
PFCOUNT hll:2026-10
```

- Set으로 하면 **합집합 비용 큼**, HLL은 **12 KB × N 병합** → 가볍다

---

## 4부. Geospatial — 위치 기반 서비스

### 개념

위도/경도를 **geohash**로 변환해 **Sorted Set의 score로 저장**. 내부적으로는 Sorted Set 그대로이므로 ZSet 명령도 함께 쓸 수 있다.

### 주요 명령

```
# 좌표 추가 (longitude, latitude, member)
GEOADD stores 127.0276 37.4979 "강남점"
GEOADD stores 127.0286 37.4988 "역삼점"
GEOADD stores 127.0304 37.4967 "삼성점"

# 두 지점 거리
GEODIST stores "강남점" "역삼점" m
# → 150.23 (미터)

# 특정 좌표 반경 N 미터 내 (최대 N개)
GEOSEARCH stores FROMLONLAT 127.0276 37.4979 BYRADIUS 500 m ASC COUNT 10

# 특정 멤버 반경
GEOSEARCH stores FROMMEMBER "강남점" BYRADIUS 500 m ASC

# 사각 영역 검색
GEOSEARCH stores FROMLONLAT 127.0 37.5 BYBOX 1000 500 m

# 좌표 조회
GEOPOS stores "강남점"
# → 1) 127.0276... 2) 37.4979...

# geohash 문자열 (base32 인코딩)
GEOHASH stores "강남점"
```

### 결과에 추가 정보 요청

```
GEOSEARCH stores FROMLONLAT 127.03 37.49 BYRADIUS 1 km
  WITHCOORD    # 좌표 포함
  WITHDIST     # 거리 포함
  WITHHASH     # geohash 포함
  ASC          # 가까운 순
  COUNT 10
```

### 전형 사용 사례

- **배달 앱**: 내 주변 매장 조회
- **차량 공유**: 가까운 차량 매칭
- **매칭 서비스**: 근처 사용자 찾기
- **IoT**: 센서 위치 추적
- **지도 기반 검색**

### 주의점

- **단일 Sorted Set**으로 저장 — 수백만 좌표 저장 시 **메모리 증가 큼**
- 대규모라면 **공간 분할**(격자 단위 Set 분리) 또는 **PostGIS/Elastic Geo**로 전환
- geohash는 **대략적**인 격자 매칭 → 경계 근처는 **반경 검색**이 더 정확

---

## 5부. 그 외 활용 패턴

### 5.1 분산 락 (SET NX EX + Redlock)

```
# 단순 락 (단일 노드 가정)
SET lock:order:123 "worker-A" NX EX 10

# 해제는 반드시 Lua 스크립트로 (내 락인지 확인)
EVAL "
  if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
  else
    return 0
  end
" 1 lock:order:123 "worker-A"
```

- **TTL 필수** — 워커 다운 시 영구 락 방지
- **단일 노드는 Failover 시 안전하지 않음** → **Redlock**(여러 노드 과반) 또는 **Redisson** 사용

### 5.2 Pub/Sub으로 캐시 무효화

```
# 서버 1: DB 업데이트 후 신호
PUBLISH cache_invalidate "user:123"

# 서버 2, 3, ...: 구독해서 로컬 캐시 비우기
SUBSCRIBE cache_invalidate
```

### 5.3 세션 저장소

```
HSET session:abc123 user_id 456 login_time <ts> cart_items 3
EXPIRE session:abc123 1800
```

- **애플리케이션 수평 확장**에 필수
- Spring Session, Express session-redis 등 통합 라이브러리 다수

### 5.4 토큰 버킷 레이트 리밋

```
# 사용자별 토큰 bucket
HSET rl:user:123 tokens 100 last_refill <ts>

# 요청 시 refill → consume → 체크 (Lua로 묶음)
```

### 5.5 Set 집합 연산으로 추천

```
# A가 본 상품
SADD user:A:viewed "product:1" "product:2" "product:3"
# B가 본 상품
SADD user:B:viewed "product:2" "product:3" "product:4"

# A는 봤지만 B는 안 본 상품 (A에게 B의 안본 상품 추천 X)
SDIFF user:A:viewed user:B:viewed

# A와 비슷한 유저가 많이 본 상품 (교집합이 큰 유저 찾기)
# → 협업 필터링 기본
```

### 5.6 Stream으로 이벤트 소싱

```
# 모든 사용자 액션을 Stream에
XADD events * action "signup" user_id 123
XADD events * action "purchase" user_id 123 amount 50000

# Consumer Group으로 분산 처리
XGROUP CREATE events billing_grp $ MKSTREAM
XREADGROUP GROUP billing_grp worker1 COUNT 10 STREAMS events >
```

### 5.7 Bitmap으로 접속 Streak

```
# 유저 123이 매일 접속했는지 (한 유저당 1비트/일)
SETBIT streak:user:123 <day_of_year> 1

# 올해 접속일 수
BITCOUNT streak:user:123

# 최근 7일 연속 접속 여부
BITCOUNT streak:user:123 <today-7> <today>
```

---

## 6부. 사이드 프로젝트 아이디어 모음

패턴을 **직접 만져볼 수 있는 아이디어**. 복잡도별로 몇 개씩.

### 🟢 쉬움 (하루~이틀)

**1. 실시간 코딩 리더보드 서비스**
- 자료 구조: **Sorted Set** (ZADD/ZREVRANGE)
- 기능:
  - 사용자 점수 등록/업데이트
  - 상위 N명 + 특정 유저 ±5등 보기
  - 주간/월간 리셋
- 포인트: ZINCRBY 원자성, 동률 처리
- 확장: 리그 분리(Bronze/Silver/Gold), 시즌제

**2. URL 단축기 + 조회수 카운터**
- 자료 구조: **String** (SET short → long), **HINCRBY** (조회수)
- 기능:
  - POST /shorten → 단축 URL 반환
  - GET /:code → 리다이렉트 + 조회수 증가
  - 통계 페이지 (일별 조회수)
- 포인트: 단축 코드 중복 처리 (SETNX), TTL
- 확장: Bitmap으로 유니크 방문자, 지역별 통계

**3. 실시간 투표 시스템**
- 자료 구조: **HINCRBY** (옵션별 득표), **Set** (중복 투표 방지)
- 기능:
  - 투표 생성/참여
  - 1인 1표 검증 (SISMEMBER + SADD)
  - 실시간 결과 조회
- 포인트: 원자성, TTL로 투표 종료

### 🟡 중간 (일주일~2주)

**4. 지역 기반 중고 거래 알림**
- 자료 구조: **GEO**, **Pub/Sub**
- 기능:
  - 유저 위치 저장
  - 새 상품 등록 시 반경 5 km 유저에게 알림
  - 좌표 기반 검색 ("내 주변 X km 내 상품")
- 포인트: GEOSEARCH, Pub/Sub 캐시 무효화
- 확장: 카테고리 필터, 거리별 정렬

**5. 실시간 트렌딩 토픽 (뉴스/트윗)**
- 자료 구조: **Sorted Set** (시간 가중치), **HyperLogLog** (유니크 언급자)
- 기능:
  - 특정 키워드 언급 시 점수 증가 (최근 시간일수록 가중치)
  - 상위 10 트렌드
  - 유니크 언급자 수 (HLL)
- 포인트: 시간 감쇠 함수, HLL vs Set 트레이드오프
- 확장: 지역별/카테고리별 트렌드

**6. 분산 작업 큐 + 재시도**
- 자료 구조: **Stream** (Consumer Group) 또는 **List** (BRPOP)
- 기능:
  - 작업 등록
  - 여러 워커 분산 처리
  - 실패 시 Dead Letter 큐로 이동
  - 작업 상태 조회
- 포인트: ACK/PEL(Pending Entries List), 재시도 전략
- 확장: 우선순위 큐 (Sorted Set), 지연 큐

**7. API Rate Limiter (SaaS 요금제)**
- 자료 구조: **Sorted Set** (슬라이딩 윈도우) + **Hash** (요금제 설정)
- 기능:
  - 요금제별 분당/시간당/일당 제한
  - 429 응답 + Retry-After 헤더
  - 사용량 조회 API
- 포인트: Lua 스크립트 원자성
- 확장: 요금제 업그레이드 시 즉시 반영

### 🔴 어려움 (한 달+)

**8. 실시간 멀티플레이어 게임 백엔드 (리더보드 + 매칭 + 세션)**
- 자료 구조: Sorted Set + GEO + Set + Hash + Pub/Sub
- 기능:
  - **매칭**: 비슷한 ELO 플레이어 매칭 (ZRANGEBYSCORE)
  - **지역 매칭**: GEO 반경 플레이어 우선
  - **방 관리**: Set (방 → 플레이어들)
  - **실시간 상태**: Hash (플레이어 → HP/위치/스코어)
  - **채팅**: Pub/Sub
  - **리더보드**: Sorted Set
- 포인트: 거의 모든 자료 구조 활용
- 확장: Pub/Sub 실시간 state sync + Stream 재접속 보장

**9. 실시간 이벤트 소싱 + CQRS 시스템**
- 자료 구조: **Stream** (이벤트 로그) + Hash/Set (뷰 모델)
- 기능:
  - 모든 상태 변경을 Stream에 기록
  - Consumer Group이 뷰 모델 생성 (현재 잔액, 통계 등)
  - Stream의 과거 이벤트 재생으로 상태 복원
- 포인트: 이벤트 소싱 패턴, Replay
- 확장: 여러 뷰 (billing, analytics 등) 병렬 소비

**10. 사용자 활동 분석 대시보드**
- 자료 구조: **Bitmap** (DAU/WAU/MAU), **HyperLogLog** (유니크 액션), **Sorted Set** (상위 사용자)
- 기능:
  - 가입/로그인/구매 이벤트 수집
  - 일별/주별/월별 DAU/WAU/MAU
  - 유니크 액션별 사용자 수
  - 가장 활동적인 유저 Top N
  - 사용자 접속 streak
- 포인트: 1000만 유저 활동을 **MB 단위**로 압축
- 확장: 코호트 분석, 리텐션

### 🎁 통합 추천 — "Redis 포트폴리오 1건"

**11. 지역 기반 미니 커뮤니티 "근방"**
- Spring Boot / FastAPI 중 하나로
- Redis 자료 구조 7가지 이상을 **의도적으로** 활용:
  - **GEO**: 위치 기반 글 조회
  - **Sorted Set**: 인기 글 랭킹 (좋아요 가중치 + 시간 감쇠)
  - **HyperLogLog**: 일별 유니크 조회수
  - **Bitmap**: 유저 활성 여부 (DAU)
  - **Hash**: 글/유저 객체
  - **Set**: 좋아요한 유저 집합
  - **Stream**: 알림 큐
  - **Pub/Sub**: 실시간 알림 전송
  - **분산 락**: 선착순 쿠폰 발급
- 포트폴리오로서 Redis 역량을 **압도적으로 보여줄 수 있음**

### 블로그 소재로도 쓸 수 있는 조합

- "Set으로 할 뻔 한 걸 **HLL**로 바꿔서 메모리 100분의 1로 줄인 이야기"
- "**Bitmap**으로 100만 유저 DAU를 125 KB에 담은 이야기"
- "**Sorted Set**으로 리더보드 + 지연 큐 + 레이트 리밋 한 번에 구현"
- "KEYS/DEL이 서비스를 멈춘 썰 → SCAN/UNLINK 전환기"

---

### 요약

- **Sorted Set 활용**: 리더보드 / 지연 큐 / 슬라이딩 윈도우 레이트 리밋 / 최근 조회 / 자동완성
- **카운팅**: INCR / HINCRBY (다중) / 시간 단위 키 + TTL / Bitmap (불리언) / Set (유니크) / HLL (대규모 근사)
- **HyperLogLog**: 12 KB로 대규모 유니크, PFMERGE로 집계. 멤버 개별 조회 X
- **Geo**: Sorted Set 기반 geohash. 매장/매칭/배달 등 위치 서비스
- **기타**: 분산 락(SET NX EX + Redlock) / Pub/Sub 캐시 무효화 / 세션 / 이벤트 소싱(Stream) / 접속 streak(Bitmap)
- **사이드 프로젝트**: 쉬움(리더보드/URL단축기/투표) → 중간(지역알림/트렌딩/작업큐/레이트리밋) → 어려움(멀티플레이어/이벤트소싱/대시보드) → 통합(지역 커뮤니티 "근방")
