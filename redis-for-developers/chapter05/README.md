### CHAPTER 5 레디스를 캐시로 사용하기

레디스의 **가장 흔한 쓰임새**가 캐시다. 하지만 "그냥 저장하고 꺼내기"로 끝나지 않는다. **어떤 전략으로 캐싱할지 / 만료된 키는 어떻게 정리되는지 / 메모리 꽉 찼을 때 어떤 키를 지울지 / 세션과는 뭐가 다른지** — 캐시 운영의 핵심 네 축을 정리.

#### 목차

- [1부. 캐싱 전략 6가지](#1부-캐싱-전략-6가지)
- [2부. 캐시 운영의 함정과 대응](#2부-캐시-운영의-함정과-대응)
- [3부. 키 만료 방식 — Passive vs Active](#3부-키-만료-방식--passive-vs-active)
- [4부. 메모리 관리와 maxmemory-policy](#4부-메모리-관리와-maxmemory-policy)
- [5부. 캐시와 세션의 차이](#5부-캐시와-세션의-차이)

---

## 1부. 캐싱 전략 6가지

### 1. Cache-Aside (Lazy Loading) — **가장 흔함**

> 애플리케이션이 **직접 캐시를 조회**하고, **miss일 때만 DB에서 조회해 캐시에 저장**.

```
Client
  ↓ GET /user/123
App
  ↓ 1. Redis GET user:123
  ├─ hit → 반환
  └─ miss
       ↓ 2. DB SELECT
       ↓ 3. Redis SET user:123 ... EX 300
       ↓ 4. 반환
```

**장점**:
- 구현 단순, **애플리케이션이 전적으로 통제**
- **필요한 데이터만** 캐시 (lazy)
- Redis 장애 시 **DB 직접 조회로 fallback** 가능

**단점**:
- **첫 요청은 항상 miss** → 지연 발생 (Cold Start)
- **데이터 불일치 가능성** — DB 업데이트 후 캐시 무효화 안 하면 stale

**실무 적용**:
```java
// Spring 예시
User user = redis.get("user:" + id);
if (user == null) {
    user = userRepository.findById(id);
    redis.set("user:" + id, user, Duration.ofMinutes(5));
}
return user;
```

### 2. Read-Through

> **캐시가 DB 조회를 "자동"으로** 처리. 애플리케이션은 캐시만 호출.

```
App → Cache
       ↓ (내부)
       └─ miss 시 DB에서 로드해 자동 저장
```

**장점**:
- 애플리케이션 코드 간결
- 캐시-DB 매핑 로직을 **캐시 계층에 집중**

**단점**:
- Redis 자체는 Read-Through를 **직접 지원 X** → **애플리케이션 또는 데이터 액세스 계층**에서 구현해야 함
- Spring `@Cacheable`, EhCache 등이 이 역할

**Spring 예시**:
```java
@Cacheable(value = "users", key = "#id")
public User findUser(Long id) {
    return userRepository.findById(id);
}
```

### 3. Write-Through

> **DB 쓰기와 캐시 쓰기를 동시에** 수행. 쓰기 지연 ↑, 하지만 **일관성 보장**.

```
App → DB UPDATE → Cache UPDATE → 응답
```

**장점**:
- **캐시 항상 최신**
- 다음 조회가 즉시 hit

**단점**:
- **쓰기 지연 증가** — DB + 캐시 둘 다 성공해야
- 캐시에 **쓰고 나서 안 읽히는 데이터**도 저장 (낭비)
- DB 쓰기 성공 + 캐시 쓰기 실패 시 **불일치**

### 4. Write-Behind (Write-Back)

> **캐시만 먼저 업데이트**하고, **DB는 비동기로** 반영.

```
App → Cache UPDATE → 즉시 응답
                        ↓
                      (비동기) Cache → DB
```

**장점**:
- **쓰기 지연 극소화**
- DB 쓰기를 배치/압축 가능

**단점**:
- **캐시 다운 시 미반영 데이터 유실** — 심각한 데이터 손실 리스크
- DB가 **잠시 stale**
- 복잡도 매우 높음

**실무**: 레디스 자체로 구현은 어려움. 대개 **메시지 큐 + Worker**로 비동기 플러시.

### 5. Write-Around

> **DB만 쓰고 캐시는 안 씀**. 조회 시 Cache-Aside로 캐시 채움.

```
Write:  App → DB (끝)
Read:   App → Cache miss → DB → Cache 저장 → 응답
```

**장점**:
- **자주 안 읽는 데이터**가 캐시 공간 차지 안 함
- 캐시 메모리 효율 ↑

**단점**:
- 방금 쓴 데이터 **첫 조회는 miss**

**적합 사례**: **쓰기 다량 + 읽기 적음** — 로그 데이터 등

### 6. Refresh-Ahead

> 캐시가 **만료 전에 미리 갱신**. 사용자가 느끼는 cold start를 완전히 숨김.

```
Cache 만료 임박 → 백그라운드로 미리 DB 조회 → 캐시 갱신 → 만료 시점에도 hit
```

**장점**:
- **일관되게 낮은 지연**
- 예측 가능한 트래픽 패턴에 강력

**단점**:
- **불필요한 갱신** 가능성 (안 쓰이는 데이터도 갱신)
- 구현 복잡

**적합 사례**: **인기 상품 상세, 홈 화면 피드** 등 **접근 패턴이 명확한 데이터**

### 전략 비교표

| 전략 | 쓰기 지연 | 읽기 지연 | 일관성 | 복잡도 | 적합 사례 |
|---|---|---|---|---|---|
| **Cache-Aside** | 영향 X | 첫 miss만 느림 | 애플리케이션 관리 | 낮음 | **범용** |
| **Read-Through** | 영향 X | 첫 miss만 느림 | 캐시 계층 관리 | 낮음 | 데이터 액세스 추상화 |
| **Write-Through** | **증가** | hit 지속 | **강함** | 중간 | 금융/일관성 중요 |
| **Write-Behind** | **매우 짧음** | hit 지속 | **약함** | 높음 | 쓰기 많고 유실 OK |
| **Write-Around** | 영향 X | 첫 miss | 중간 | 낮음 | 쓰기 多 읽기 少 |
| **Refresh-Ahead** | 영향 X | **매우 낮음** | 중간 | 높음 | 인기 데이터 예측 가능 |

---

## 2부. 캐시 운영의 함정과 대응

### 1. Cache Stampede (Thundering Herd)

> **인기 키의 TTL 만료 순간**, 수많은 요청이 **동시에 DB로 몰림** → DB 과부하.

```
10,000 QPS에 TTL 1분인 인기 키
→ TTL 만료 순간, 1초 안에 ~10,000 요청이 모두 miss → DB 폭주
```

**대응**:
1. **분산 락**: miss 발생 시 **한 요청만 DB 조회**, 나머지는 대기 또는 stale 반환
   ```
   SET lock:refresh:key NX EX 5
   ```
2. **PER (Probabilistic Early Refresh)**: TTL 만료 전에 **확률적으로 미리 갱신**
3. **Refresh-Ahead** 전략

### 2. Cache Penetration

> **DB에도 없는 데이터**를 반복 조회 → 매번 cache miss + DB 조회 → **DB에 공격 가능성**.

```
악의적 사용자가 존재하지 않는 user_id=99999999를 1만 번 호출
→ 매번 cache miss + DB 조회
```

**대응**:
1. **Null 캐싱**: "없음"도 짧은 TTL로 캐시
   ```
   SET user:99999 "NOT_FOUND" EX 60
   ```
2. **Bloom Filter**: "이 ID 존재할 가능성" 1차 필터링
3. **입력 검증**: 비정상 범위 ID 즉시 거부

### 3. Cache Avalanche

> **대량의 키가 동시에 만료** → DB 폭주.

```
새벽 2시에 batch로 수백만 키를 한꺼번에 저장 → 1시간 후 모두 동시 만료
```

**대응**:
1. **TTL에 ± 랜덤값 추가**:
   ```
   SET key value EX (3600 + random(0, 300))
   ```
2. **키 그룹별 TTL 분산**

### 4. Hot Key

> **특정 키 하나에 트래픽이 집중**. 레디스가 단일 스레드라 **네트워크 I/O + 명령 처리에서 병목**.

**대응**:
1. **로컬 캐시 2단 (L1/L2)**: 애플리케이션 로컬 + Redis
2. **키 샤딩**: `product:1234` → `product:1234:shard1`, `shard2`, ... 로 분할 저장
3. **읽기 레플리카 활용**

### 5. Big Key

> **단일 키의 값이 너무 큼** (수 MB, 수백만 원소 Hash/Set) → 조회/삭제가 메인 스레드 블록.

**대응**:
1. **값 분할**: 큰 Hash를 여러 Hash로
2. **읽기는 SCAN 계열**
3. **삭제는 UNLINK**
4. 사전 탐지: `MEMORY USAGE key` 또는 `redis-cli --bigkeys`

---

## 3부. 키 만료 방식 — Passive vs Active

레디스에서 **TTL 걸린 키는 두 가지 방식**으로 삭제된다. **단일 방식만 사용하지 않는 이유**가 핵심.

### Passive Expiration (수동적, Lazy)

> 키에 **접근할 때** 레디스가 "이 키 TTL 지났네?" 체크 후 **삭제 + miss 반환**.

```
GET expired_key
   ↓ 레디스 내부
   ↓ check: now > expire_at?
   ↓ yes → DEL expired_key
   ↓ return nil
```

**장점**:
- **접근 비용에 포함** — CPU 낭비 없음
- 삭제 **정확한 시점**

**단점**:
- **접근 안 하면 영원히 안 지워짐** → **메모리 누수**
- 접근하지 않는 만료 키가 쌓이면 **메모리 폭발**

### Active Expiration (능동적, Periodic Scan)

> 레디스가 **주기적으로** TTL 걸린 키 공간을 **샘플링해서 만료된 키 삭제**.

**동작 (Redis 소스 기반)**:

```
1. 매 100ms마다 실행 (hz 설정값 기준)
2. 각 DB마다:
   a. TTL 있는 키 중에서 20개 랜덤 샘플링
   b. 만료된 키 삭제
   c. 샘플 중 25% 이상이 만료였다면 → 1단계 반복 (더 많이 지움)
   d. 전체 CPU 사용률의 25% 넘어가면 중단
```

**장점**:
- 접근 안 하는 키도 **점진적으로** 정리됨
- 메모리 누수 방지

**단점**:
- **CPU 사용** — 활동이 적을 때도 백그라운드 작업
- **확률적** — 모든 만료 키를 즉시 지우지는 않음

### 왜 두 방식을 함께 쓰는가

- **Passive만**: 접근 안 하는 키 영구 잔존 → 메모리 폭발
- **Active만**: 모든 TTL 키를 매번 전수 조사 = **O(N) 블록**, 레디스 설계 철학 위배

→ **두 방식 조합이 "CPU 효율 + 메모리 회수" 균형**:
- 접근하는 키 → Passive로 즉시 삭제
- 접근 안 하는 키 → Active가 점진 샘플링으로 삭제

### 관련 설정

| 설정 | 기본값 | 의미 |
|---|---|---|
| **hz** | `10` | 1초당 Active 실행 횟수 (10 = 100ms마다) |
| **dynamic-hz** | `yes` | 접속 많으면 hz 자동 증가 |
| **active-expire-effort** | `1` | Active 공격성 (1~10, 높을수록 CPU 사용 ↑ 메모리 회수 ↑) |
| **lazyfree-lazy-expire** | `no` | Active 만료 삭제도 **백그라운드 스레드로** → 메인 블록 방지 |

**운영 권장**:
```
hz 10
dynamic-hz yes
lazyfree-lazy-expire yes    # 큰 Hash/Set의 TTL 만료 시 블록 방지
```

### 복제 환경에서의 만료

중요한 포인트: **레플리카는 자체적으로 키를 삭제하지 않음** (Redis 3.2+). 삭제는 **마스터만** 수행하고, **DEL 명령을 레플리카에 복제**해서 동기화.

이유: **마스터와 레플리카의 만료 시점이 조금만 달라도 데이터 불일치** → 마스터가 "진실의 원천" 역할.

### 요약 비교

| 축 | Passive | Active |
|---|---|---|
| **트리거** | 키 접근 시 | 주기적 (hz) |
| **대상** | 접근한 키 | 랜덤 샘플 20개 |
| **CPU 사용** | 접근당 소량 | 백그라운드 지속 |
| **메모리 회수** | 접근해야만 | 샘플링으로 점진적 |
| **단독 사용 시 문제** | 메모리 누수 | 전수 조사 비용 |
| **운영** | 자동 | hz, active-expire-effort로 튜닝 |

---

## 4부. 메모리 관리와 maxmemory-policy

### maxmemory 설정의 의미

```
maxmemory 4gb
```

- **레디스가 사용할 최대 메모리 상한**
- 도달 시 → `maxmemory-policy`에 따라 **eviction** 수행
- **미설정 시 OS 메모리 다 쓰다 OOM Kill** → 실무에서는 반드시 설정

### 8가지 정책

```
maxmemory-policy allkeys-lru
```

#### 1. noeviction (기본값)

- 메모리 초과 시 **쓰기 명령 모두 실패** (`OOM command not allowed`)
- 읽기는 가능
- **데이터 손실 절대 금지 상황** (영구 저장소 용도)
- **캐시로 쓸 땐 매우 위험** — 쓰기 거부 = 서비스 영향

#### 2. allkeys-lru

- **모든 키 중** 가장 오래 사용 안 된 키 제거 (**Least Recently Used**)
- **캐시 운영의 사실상 표준**
- 자주 쓰는 데이터는 자동으로 유지, 식어가는 데이터는 제거

**내부 동작 (근사 LRU)**:
- 정확한 LRU는 전체 키 리스트 필요 → 비용 ↑
- 레디스는 **랜덤 샘플 N개 중 가장 오래된 것 제거** (기본 N=5)
- `maxmemory-samples` 조정으로 정확도 ↔ 성능 트레이드오프

#### 3. allkeys-lfu (Redis 4.0+)

- **모든 키 중** 가장 적게 사용된 키 제거 (**Least Frequently Used**)
- **접근 빈도**를 반영 (최근성과 다름)
- 자주 쓰이는 "핫 키"는 **오래 안 쓰여도 유지**
- **LRU로는 못 지키는 핫 데이터**를 지키고 싶을 때

**내부**:
- 각 키에 **접근 카운터**(Logarithmic Counter) 유지
- 시간이 지나면 **카운터 감쇠** → "옛날엔 핫했지만 지금은 안 쓰이는" 키도 결국 제거

#### 4. allkeys-random

- 랜덤 제거
- 거의 안 씀 (특수한 균등 접근 패턴 가정)

#### 5. volatile-lru

- **TTL 설정된 키** 중 LRU 제거
- **TTL 없는 키는 "영구 데이터"로 간주해 유지**
- **캐시 + 영구 데이터 공존**할 때 안전

#### 6. volatile-lfu

- TTL 설정된 키 중 LFU 제거
- 위와 동일 철학 + LFU 선호

#### 7. volatile-random

- TTL 설정된 키 중 랜덤 제거

#### 8. volatile-ttl

- **TTL이 가장 적게 남은** 키부터 제거
- 어차피 곧 지워질 키부터 치움
- **최근성/빈도 상관없이** 순수 TTL 기반

### 정책 선택 가이드

| 상황 | 추천 정책 |
|---|---|
| **순수 캐시 (전부 TTL)** | **`allkeys-lru`** |
| 접근 빈도가 중요 (핫 키 많음) | `allkeys-lfu` |
| **캐시 + 영구 데이터 공존** | **`volatile-lru`** |
| TTL이 비즈니스 로직의 핵심 (지연 큐 등) | `volatile-ttl` |
| 데이터 손실 절대 불가 | `noeviction` (메모리 늘려야 함) |

### LRU vs LFU 선택

**LRU 선호**:
- 최근 접근 데이터가 다시 접근될 가능성 ↑ (**시간 지역성**)
- 뉴스/소셜 피드, 사용자 세션 등

**LFU 선호**:
- 소수의 **인기 데이터**가 반복 접근됨
- 베스트셀러, 인기 상품 상세
- "가끔 몰리는 트래픽에 밀려 핫 데이터가 LRU로 제거되는 상황" 방지

### maxmemory-samples 튜닝

```
maxmemory-samples 5      # 기본
```

- Eviction 시 N개 샘플링 → 가장 적합한 것 제거
- **5 → 10으로 올리면 정확도 ↑ CPU ↑**
- 보통 **5~10** 범위에서 운영

### Eviction 모니터링

```
INFO stats
# evicted_keys:12345      ← Eviction 발생 수
# keyspace_misses:67890
```

- `evicted_keys` 급증 → 메모리 부족 신호
- **메모리 상향 또는 TTL 재검토**

---

## 5부. 캐시와 세션의 차이

레디스로 **둘 다 저장 가능**하지만 **운영 전략은 완전히 다르다**. 혼동하면 **세션이 날아가서 사용자가 로그아웃되는 사고** 발생.

### 캐시 (Cache)

| 성질 | 특징 |
|---|---|
| **데이터의 본질** | **원본의 사본** — DB에서 재생성 가능 |
| **손실의 영향** | 재조회로 복구, **사용자 영향 없음** |
| **TTL** | 보통 짧게 (초~분) |
| **Eviction** | **적극적** — 메모리 꽉 차면 지워도 OK |
| **일관성** | **최종 일관성** 허용 |
| **영속성** | 보통 **불필요** (RDB/AOF 꺼도 됨) |

**예**:
- 상품 상세, 사용자 프로필 캐시
- API 응답 캐시
- SQL 쿼리 결과 캐시

### 세션 (Session)

| 성질 | 특징 |
|---|---|
| **데이터의 본질** | **사용자의 상태** — **원본 없음** (레디스가 유일한 저장소) |
| **손실의 영향** | **로그아웃**, 장바구니 소실, 사용자 영향 큼 |
| **TTL** | 보통 길게 (30분~며칠) + **활동 시 갱신** |
| **Eviction** | **보수적** — 지워지면 사용자 영향 |
| **일관성** | **강한 일관성** 필요 (로그인 상태 등) |
| **영속성** | 보통 **필요** (AOF 등으로 재시작 생존) |

**예**:
- 로그인 토큰
- OAuth 세션
- 장바구니 (아직 결제 안 한 상태)

### 비교표

| 축 | 캐시 | 세션 |
|---|---|---|
| **원본** | DB에 있음 | **레디스가 원본** |
| **데이터 손실 시** | 성능 영향 (재조회) | **사용자 영향** (로그아웃 등) |
| **TTL** | 짧게 (초~분) | 길게 (분~일) |
| **TTL 갱신** | 보통 안 함 | **매 요청마다 갱신** (sliding) |
| **maxmemory-policy** | **`allkeys-lru`** | **`volatile-lru`** (TTL 없는 키 보호) 또는 **`noeviction`** |
| **영속성** | 없어도 OK | **AOF 권장** |
| **인스턴스 분리** | 캐시 전용 | **세션 전용** (권장) |

### 왜 **분리**하는가 — 함께 쓰면 생기는 문제

**문제 상황**: 캐시 + 세션을 한 인스턴스에 저장 + `maxmemory-policy allkeys-lru`.

```
캐시 트래픽 폭증 → 메모리 꽉 참 → LRU로 오래된 키 Eviction
→ 하필 Eviction된 키가 "세션"
→ 사용자가 로그아웃 당함
```

**해결책 (세 가지)**:

1. **인스턴스 분리** (최선)
   - Redis-Cache 인스턴스 + Redis-Session 인스턴스
   - 각각 다른 maxmemory, 다른 policy

2. **같은 인스턴스에서 policy 분리**
   - `maxmemory-policy volatile-lru`
   - 캐시만 TTL 설정 (eviction 대상)
   - 세션은 TTL 설정하되 **긴 TTL + sliding**

3. **DB 번호 분리**
   - 세션은 DB 0, 캐시는 DB 1
   - 하지만 **maxmemory는 인스턴스 전체 공유** → eviction은 여전히 발생
   - **임시방편일 뿐 근본 해결 X**

### 세션 저장 베스트 프랙티스

```java
// Spring Session + Redis 예시
HSET session:abc123 user_id 456 login_time 1697000000 cart_items 3
EXPIRE session:abc123 1800        # 30분 TTL

// 요청마다 TTL 갱신 (sliding)
EXPIRE session:abc123 1800
```

- **Hash로 묶어 저장** — 세션 속성을 한 키에
- **EXPIRE 매 요청마다 갱신** → 활성 사용자 로그아웃 안 됨
- **AOF 활성화** — 재시작 후에도 세션 유지
- **Redlock 또는 Sticky Session** — 세션 쓰기 경쟁 조건 방지

### 캐시 저장 베스트 프랙티스

```
SET cache:product:1234 "..." EX 300
```

- **짧은 TTL** (수 분)
- `allkeys-lru` 또는 `allkeys-lfu`
- **AOF 비활성** 가능 (성능 ↑)
- **대형 데이터는 압축 저장** (gzip 등)
- 캐시 miss 로그/지표로 **hit ratio** 모니터링 — 70% 미만이면 전략 재검토

---

### 요약

- **캐싱 전략 6가지**: Cache-Aside(가장 흔함) / Read-Through / Write-Through(일관성 ↑) / Write-Behind(지연 ↓ 손실 리스크) / Write-Around(쓰기 多 읽기 少) / Refresh-Ahead(인기 데이터)
- **운영 함정 5가지**: Stampede(분산 락) / Penetration(Null 캐싱, Bloom Filter) / Avalanche(TTL ± 랜덤) / Hot Key(2단 캐시, 샤딩) / Big Key(분할, UNLINK)
- **키 만료 — Passive + Active 둘 다**:
  - **Passive**: 접근 시 체크 → 메모리 누수 가능
  - **Active**: 100ms마다 랜덤 20개 샘플링 → CPU 사용
  - 조합으로 CPU/메모리 균형. `hz`, `active-expire-effort`, `lazyfree-lazy-expire`로 튜닝
  - 레플리카는 자체 삭제 X — 마스터의 DEL 복제
- **maxmemory-policy 8가지**:
  - 순수 캐시 → **`allkeys-lru`** (표준) / 핫 키 중요 → `allkeys-lfu`
  - 캐시+영구 공존 → **`volatile-lru`** / TTL 로직 중심 → `volatile-ttl`
  - 영구 저장소 → `noeviction` (쓰기 거부 리스크)
  - LRU vs LFU는 **시간 지역성 vs 접근 빈도**로 선택
- **캐시 vs 세션**:
  - 캐시 = **원본의 사본**, 손실 OK, 짧은 TTL, allkeys-lru
  - 세션 = **원본**, 손실 = 사용자 영향, 긴 TTL + sliding, volatile-lru 또는 **인스턴스 분리**
  - 함께 쓰면 **캐시 트래픽에 세션이 Eviction되는 사고** → **분리가 안전**
