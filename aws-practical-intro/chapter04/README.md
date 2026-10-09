### 4장. RDS

## 1부. 데이터 웨어하우스 (Data Warehouse)

#### 개념

**분석/보고 전용 대용량 데이터 저장소**. 운영 DB(OLTP)와 분리된 별도 시스템.

**왜 운영 DB와 분리하나?**
- 운영 DB에서 복잡한 분석 쿼리 돌리면 **운영 성능 저하**
- 운영 DB는 정규화 (수많은 테이블), 분석엔 비효율
- 리포트 쿼리는 수십 분 걸릴 수 있음 — 운영 응답에 영향

**데이터 웨어하우스 특징**:
- **컬럼 지향 저장(Column-Oriented)** — 분석 쿼리(집계)에 유리
- **대용량** (TB~PB)
- **배치 로딩** 중심 (실시간 쓰기 X, ETL로 적재)
- **비정규화된 스키마** (Star schema, Snowflake schema)
- **복잡한 JOIN/aggregation 최적화**

**AWS**: **Redshift** (관리형 데이터 웨어하우스)

#### Data Warehouse / Data Lake / Lakehouse 비교

| | **Data Warehouse** | **Data Lake** | **Lakehouse** |
|---|------------------|-------------|------------|
| 저장 형식 | 정제된 테이블 (SQL) | 원시 데이터 (JSON, CSV, Parquet, 이미지…) | 둘 다 |
| 스키마 | 사전 정의 (schema-on-write) | 유연 (schema-on-read) | 둘 다 |
| 사용처 | BI, 리포트 | ML, 데이터 과학 | 통합 |
| AWS 제품 | **Redshift** | **S3 + Glue + Athena** | **Lake Formation** |

#### 아키텍처 패턴

```
운영 DB (RDS)            데이터 레이크 (S3)
   OLTP                    원시 데이터
     │                        │
     └── ETL/CDC ──────┬───────┘
                       │
                       ▼
                Data Warehouse (Redshift)
                   정제된 분석 데이터
                       │
                       ▼
                BI / 대시보드 (QuickSight, Tableau)
```

---

## 2부. OLTP vs OLAP ⭐

**가장 중요한 데이터베이스 분류 축**.

| | **OLTP** | **OLAP** |
|---|---------|----------|
| **풀네임** | Online Transaction Processing | Online Analytical Processing |
| **목적** | 트랜잭션 처리 | 분석/리포트 |
| **쿼리** | 짧은 INSERT/UPDATE/DELETE/SELECT | 긴 SELECT (GROUP BY, JOIN) |
| **데이터 양** | 작은 레코드, 많은 트랜잭션 | 큰 집계, 적은 쿼리 수 |
| **응답 시간** | **ms 단위** | 초~분 단위 OK |
| **동시성** | 수많은 사용자 (수천~수만 TPS) | 분석가 (수십 명) |
| **저장 방식** | **Row-oriented** | **Column-oriented** |
| **스키마** | 정규화 (3NF) | 비정규화 (Star/Snowflake) |
| **데이터 신선도** | 실시간 | 일/시간 단위 지연 OK |
| **대표 제품** | MySQL, PostgreSQL, Oracle | Redshift, BigQuery, Snowflake |
| **AWS 제품** | **RDS, Aurora** | **Redshift** |

#### Row-oriented vs Column-oriented 차이

**OLTP 쿼리 예시**:
```sql
SELECT * FROM users WHERE id = 123;    -- 1개 레코드 전체 (Row-oriented 유리)
UPDATE users SET name = 'x' WHERE id = 123;
```

**OLAP 쿼리 예시**:
```sql
SELECT country, SUM(amount) FROM orders    -- amount 컬럼만 전체 스캔
WHERE year = 2024
GROUP BY country;
```

**저장 레이아웃**:
```
Row-oriented (OLTP):
[id=1, name=alice, age=30] [id=2, name=bob, age=25] [id=3, ...]
└── 1개 레코드 전체를 한 곳에 ──┘

Column-oriented (OLAP):
id:     [1, 2, 3, 4, ...]
name:   [alice, bob, charlie, ...]
age:    [30, 25, 40, ...]
└── 1개 컬럼을 한 곳에 ──┘
```

→ OLAP 쿼리는 **필요한 컬럼만** 스캔 (I/O 효율). 압축률도 높음 (같은 타입 모여있음).

#### 혼용 시나리오

- **HTAP** (Hybrid Transactional/Analytical Processing) — 둘 다 지원
- 예: Google Spanner, AWS Aurora (어느 정도 OLAP도)
- 하지만 **대부분 분리하는 게 성능 유리**

---

## 3부. DB 백업 원리

### RDS 백업 2가지

#### ① 자동 백업 (Automated Backup)

- 설정된 **백업 윈도우**에 매일 1회 **자동 Snapshot** 생성
- **보관 기간 1~35일** (설정)
- **Point-in-Time Recovery (PITR)** — 5분 단위 특정 시점 복원 가능
- 자동 삭제 (보관 기간 지나면)

**작동 원리**:
```
매일 Snapshot (전체 백업, S3 저장)
    +
Transaction Log (WAL)를 5분마다 S3로 전송
    =
PITR: 가장 가까운 Snapshot → + Transaction Log Replay → 원하는 시점 복원
```

#### ② 수동 Snapshot (Manual Snapshot)

- 사용자가 명시적 생성
- **영구 보관** (명시적 삭제까지)
- 다른 **Region으로 복사** 가능
- 다른 **AWS 계정으로 공유** 가능

**사용 사례**:
- 중요 변경 전 백업 (스키마 변경, 대량 삭제)
- 장기 보관 (법적 요건)
- 환경 복제 (prod → stage)

### 백업 원리 — 기술적 디테일

#### Full backup vs Incremental backup

- **Full backup**: 전체 데이터 복사 (초기 snapshot)
- **Incremental backup**: 이전 백업 **이후 변경된 블록만** 저장
- RDS 자동 백업: 첫 번째만 full, 이후는 incremental → **디스크 효율**

#### Copy-on-Write Snapshot

```
Snapshot 생성 요청
    ↓
즉시 "snapshot 완료" (metadata만 생성, 수 초)
    ↓
백그라운드로 S3에 블록 복사
    ↓
운영 중 블록 수정 시: 수정 전 블록만 백업 (COW)
```

→ **운영 중단 없이** snapshot 가능.

#### Transaction Log Backup (PITR의 핵심)

- DB는 모든 변경을 **WAL (Write-Ahead Log)** 에 먼저 기록
- RDS가 WAL을 **5분마다 S3로 전송**
- 복원 시:
  1. 가장 가까운 전체 Snapshot 복원
  2. WAL을 **순서대로 replay** (원하는 시점까지)
  3. 그 시점의 상태로 복원 완료

→ 과거 "오후 3시 15분 상태" 복원 가능.

### Multi-AZ vs Snapshot — 혼동 금지

| | **Multi-AZ** | **Snapshot** |
|---|-----------|-------------|
| 목적 | **고가용성 (HA)** | **재해 복구 (DR)** |
| 복제 방식 | **동기 복제** | 비동기 (S3로 전송) |
| 복제 대상 | 다른 AZ의 standby 인스턴스 | S3 |
| Failover | **자동** (분 단위) | 수동 복원 |
| 데이터 손실 | 거의 없음 (동기) | 최대 5분 (PITR) |
| 추가 비용 | DB 인스턴스 2배 | S3 저장 비용 |

→ **둘 다 필요**. HA는 즉시 장애 대응, DR은 데이터 손실 복구.

### 백업 Best Practice

- [ ] 자동 백업 보관 기간 **최소 7일** (가능하면 35일)
- [ ] 주요 변경 전 **수동 Snapshot** (롤백 안전망)
- [ ] 중요 Snapshot은 **다른 Region으로 복사** (Region 전체 장애 대비)
- [ ] 복원 테스트 정기 수행 (복원 안 되는 백업은 백업이 아님)
- [ ] 암호화 Snapshot 활용 (KMS)

---

## 4부. ElastiCache (엘라스틱캐시) ⭐

**AWS 관리형 인메모리 캐시 서비스**. DB 부하 분산 + 응답 속도 개선.

#### 왜 캐시가 필요한가

```
Without Cache:
App → DB (매번 50 ms)       → 1000 req/s = DB에 1000 TPS

With Cache (hit ratio 90%):
App → Cache (0.5 ms) ─ 90%
    └→ DB (50 ms) ─────── 10%  → DB에 100 TPS
```

→ **DB 부하 10%로 감소**, 평균 응답 10x 빠름.

#### Redis vs Memcached (ElastiCache 2가지 엔진)

| 항목 | **Redis** | **Memcached** |
|-----|---------|-------------|
| **자료구조** | String, List, Set, Hash, **Sorted Set**, Stream, Bitmap, HyperLogLog | **String만** (key-value) |
| **Persistence** | **지원** (RDB snapshot, AOF log) | ❌ 메모리만 (재시작 시 전부 소실) |
| **Replication** | **지원** (Primary-Replica) | ❌ |
| **Cluster 모드** | **지원** (sharding + replication) | 지원 (sharding만) |
| **Multi-threaded** | ❌ (단일 스레드 — 7.0+ 일부 멀티) | ✅ |
| **Pub/Sub** | ✅ | ❌ |
| **Lua scripting** | ✅ | ❌ |
| **트랜잭션** | ✅ (MULTI/EXEC) | ❌ |
| **메모리 효율** | 중간 | **매우 좋음** |
| **단순함** | 복잡 | **매우 단순** |

**선택 가이드**:
- **단순 key-value만**, 여러 CPU 활용 → **Memcached**
- **복잡한 자료구조**, Persistence, Pub/Sub, 분산 lock → **Redis** (대부분)

실무에선 **Redis가 압도적**. Memcached는 매우 단순한 캐시용으로만.

#### 캐시 전략 3가지

**① Cache-Aside (Lazy Loading)** — 가장 일반적

```
read:
  App → Cache 조회
    ├─ Hit → 반환
    └─ Miss → DB 조회 → Cache 저장 → 반환

write:
  App → DB 쓰기 → Cache 삭제 (invalidate)
```

- **장점**: 캐시가 다운돼도 DB로 fallback. 쓰이는 데이터만 캐시.
- **단점**: 첫 조회는 miss (느림). 데이터 오래될 수 있음 (TTL 관리 필요).

**② Write-Through** — 쓰기 시 즉시 캐시

```
write:
  App → DB + Cache 동시 쓰기

read:
  App → Cache → 항상 fresh
```

- **장점**: 캐시가 항상 최신.
- **단점**: 쓰기 성능 느림. 안 쓰는 데이터도 캐시 차지.

**③ Write-Behind (Write-Back)** — 비동기 쓰기

```
write:
  App → Cache (즉시 응답)
         ↓
         비동기로 DB 반영
```

- **장점**: 쓰기 매우 빠름.
- **단점**: Cache 다운 시 데이터 손실 위험.

#### ElastiCache 사용 사례

| 사용처 | 설명 | Redis 자료구조 |
|------|------|-------------|
| **세션 저장소** | Spring Session 등 | Hash |
| **DB 쿼리 결과 캐시** | 자주 조회되는 데이터 | String (JSON) |
| **Leaderboard** | 실시간 순위 | **Sorted Set** |
| **Rate Limiting** | API 호출 횟수 제한 | String + TTL |
| **분산 Lock** | 멀티 서버 간 동기화 | SETNX + TTL |
| **Pub/Sub** | 실시간 알림 | Pub/Sub |
| **지역 기반 쿼리** | "근처 가게 찾기" | Geo (Sorted Set 기반) |
| **중복 제거** | 신규 user 체크 | Set, HyperLogLog |

#### 캐시 주의점

**(1) Cache Invalidation** — "데이터 바뀌었을 때 캐시 어떻게 지울지"
- Phil Karlton: "컴퓨터 과학에서 어려운 두 가지: cache invalidation과 네이밍"
- 전략: TTL + 명시적 삭제 조합

**(2) Thundering Herd (Cache Stampede)**
- 캐시 만료 순간 **모든 요청이 DB에 몰림**
- 해결: Random TTL, Lock + Rebuild, Stale-while-revalidate

**(3) Hot Key**
- 특정 key에 트래픽 집중 → 해당 Redis 노드 과부하
- 해결: Local cache (앱 메모리) + ElastiCache 2단계, Key sharding

**(4) 메모리 압박**
- 가득 차면 LRU/LFU 등 **Eviction Policy**로 데이터 쫓아냄
- 설정: `maxmemory-policy` (volatile-lru, allkeys-lru 등)

**(5) Serialization 비용**
- Redis는 string만 저장 → 객체는 JSON/Protobuf 등으로 변환
- 큰 객체는 serialization 비용 ↑

#### Spring Boot + ElastiCache

```yaml
spring:
  redis:
    host: my-cluster.xxx.cache.amazonaws.com
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 20
```

```java
@Cacheable(value = "users", key = "#id")
public User findUser(Long id) {
    return userRepo.find(id);  // 캐시 miss 시만 호출
}

@CacheEvict(value = "users", key = "#user.id")
public void update(User user) {
    userRepo.save(user);
}
```

→ Spring의 `@Cacheable` + Redis = **코드 변경 최소화**로 캐시 적용.

---

## 5부. 종합 — RDS + Redshift + ElastiCache 함께 쓰기

**일반적 아키텍처**:

```
사용자 요청
    ↓
Application (EC2)
    ├─ 세션/핫 데이터 → ElastiCache (Redis)
    ├─ 트랜잭션 데이터 → RDS (MySQL/PostgreSQL)   ← OLTP
    │                     │
    │                     └─ Snapshot/Multi-AZ (백업/HA)
    │
    └─ (ETL/CDC로)         → Redshift             ← OLAP
                              │
                              └─ BI 대시보드 (QuickSight)
```

**각 레이어의 역할**:
- **ElastiCache**: 응답 속도 (ms → μs)
- **RDS**: 트랜잭션 신뢰성 (ACID)
- **Redshift**: 대용량 분석 (PB급 집계)

---

## 참고 자료

- [Amazon RDS User Guide](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/)
- [Amazon Redshift](https://aws.amazon.com/redshift/)
- [Amazon ElastiCache](https://aws.amazon.com/elasticache/)
- [AWS Database Comparison](https://aws.amazon.com/products/databases/)
- Redis 공식 문서 — [redis.io](https://redis.io/)
