### 9장. DynamoDB

## 1부. DynamoDB 개념

#### DynamoDB란

AWS의 **완전 관리형 NoSQL 데이터베이스**. "Amazon이 자사 쇼핑몰용으로 개발한 Dynamo 논문의 상업화 버전".

**핵심 특징**:
- **서버리스** — 서버 관리 X, 자동 확장
- **Single-digit millisecond 응답** (한 자릿수 ms)
- **사실상 무제한 확장** (수백 TB까지 테스트됨)
- **Multi-AZ 자동 복제** (3개 AZ)
- **Key-Value + Document** 모델 (JSON-like)
- **스키마 유연** (각 item마다 attribute 달라도 됨)
- **ACID 트랜잭션** 지원 (TransactWriteItems/TransactGetItems)

#### RDBMS vs DynamoDB

| 항목 | **RDBMS** (RDS) | **DynamoDB** |
|------|----------------|--------------|
| 데이터 모델 | 테이블 + Row + Column | 테이블 + Item + Attribute |
| 스키마 | 고정 | **유연** (item마다 다를 수 있음) |
| JOIN | ✅ 지원 | ❌ 없음 (denormalization 필요) |
| 트랜잭션 | ✅ ACID | ✅ 제한된 ACID (최대 100 item) |
| 쿼리 | 유연한 SQL | **Partition Key 필수** |
| 확장 | Vertical (주로) | **Horizontal 자동** |
| 지연 | 수 ms~수십 ms | **< 10 ms** |
| 설계 접근 | **정규화** | **비정규화 + 접근 패턴 중심** |

#### 데이터 모델

```
Table: Orders
┌─────────────────────────────────────────────────────────┐
│ Item 1:                                                  │
│   UserId:    "user-123"    ← Partition Key (필수)        │
│   OrderId:   "order-abc"   ← Sort Key (optional)         │
│   Amount:    50000          ← Attribute                  │
│   Status:    "PAID"                                      │
├─────────────────────────────────────────────────────────┤
│ Item 2:                                                  │
│   UserId:    "user-123"                                  │
│   OrderId:   "order-def"                                 │
│   Amount:    30000                                       │
│   Items:     [{"name": "shirt", "price": 30000}]  ← nested│
└─────────────────────────────────────────────────────────┘
```

- **Table**: RDBMS의 테이블과 비슷
- **Item**: Row에 해당 (JSON-like)
- **Attribute**: Column에 해당
- **Primary Key**: 반드시 지정 (Partition Key or Partition Key + Sort Key)

---

## 2부. Partition Key와 Composite Key ⭐

### Primary Key 2가지 종류

#### ① Simple Primary Key — Partition Key만

```
Table: Users
Primary Key: UserId (Partition Key)

Items:
  { UserId: "user-123", Name: "alice", Age: 30 }
  { UserId: "user-456", Name: "bob",   Age: 25 }
```

- **각 item은 고유한 Partition Key**
- 중복 불가 (같은 UserId는 하나만)
- UserId로만 조회 가능

#### ② Composite Primary Key — Partition Key + Sort Key ⭐

```
Table: Orders
Primary Key: UserId (Partition Key) + OrderId (Sort Key)

Items:
  { UserId: "user-123", OrderId: "order-001", Amount: 50000 }
  { UserId: "user-123", OrderId: "order-002", Amount: 30000 }
  { UserId: "user-456", OrderId: "order-001", Amount: 20000 }  ← PK는 중복, SK는 다름
```

- **같은 Partition Key 안에서 Sort Key로 정렬 저장**
- Partition Key + Sort Key 조합이 유일하면 됨
- **범위 쿼리(Range Query)** 가능:
  - "user-123의 주문 중 OrderId > order-001"
  - "user-123의 2024-01 ~ 2024-02 주문"

### Partition Key의 역할

```
UserId (Partition Key)
    ↓
Hash 함수
    ↓
Partition 1 ─┐
Partition 2 ─┤  ← 물리적 저장 서버
Partition 3 ─┘
```

- **Hash로 어느 파티션(서버)에 저장될지 결정**
- 파티션 당 **최대 3000 RCU, 1000 WCU, 10 GB** 제한
- **다양한 Partition Key 값 → 균등 분산 → 성능 ↑**

#### ⚠️ Hot Partition 문제

**Bad Partition Key**:
```
PK: "ALL_USERS"   ← 모든 데이터가 같은 PK → 1개 파티션 집중 → 느림!
PK: Date          ← 오늘자 요청 폭주 → Hot
PK: Status        ← "ACTIVE" 하나에 몰림
```

**Good Partition Key**:
```
PK: UserId        ← 사용자별 분산 ✅
PK: UUID          ← 완전 랜덤 ✅
PK: CompositeKey  ← "country#userId" 조합
```

→ **고유성 높고, 접근 분산되는 값**을 Partition Key로.

### Sort Key의 활용

**효율적 범위 쿼리**:
```python
# user-123의 2024-01 주문
response = table.query(
    KeyConditionExpression='UserId = :u AND begins_with(OrderDate, :d)',
    ExpressionAttributeValues={':u': 'user-123', ':d': '2024-01'}
)
```

**정렬 저장 활용**:
- Sort Key가 timestamp → 자동 시간순
- Sort Key가 "status#timestamp" → 상태별 그룹 + 시간순

**Overloaded Key** (고급):
```
PK: UserId
SK: "ORDER#2024-01-15"
SK: "ADDRESS#home"
SK: "PAYMENT#card-01"
```
- 같은 테이블에 **다양한 타입 혼재** → Single Table Design 패턴

---

## 3부. Local Secondary Index vs Global Secondary Index ⭐

**Primary Key가 아닌 다른 attribute로 쿼리하고 싶을 때** → **Secondary Index**.

### Local Secondary Index (LSI, 로컬 보조 인덱스)

```
Base Table: Orders
PK: UserId
SK: OrderId

LSI: UserOrderByDate
PK: UserId   ← 같은 Partition Key
SK: OrderDate ← 다른 Sort Key
```

**특징**:
- **같은 Partition Key, 다른 Sort Key**
- **Base 테이블 생성 시에만** 만들 수 있음 (나중에 추가 불가)
- **테이블당 최대 5개**
- **Strongly Consistent Read 가능**
- Base 테이블의 **파티션에 "함께 저장"** → 파티션당 10 GB 제한에 포함
- **별도 Throughput 없음** (Base 테이블의 RCU/WCU 공유)

**사용 사례**:
- "UserId로 주문 조회하되, OrderDate 순서로"
- "UserId의 OrderAmount 상위 N개"

### Global Secondary Index (GSI, 글로벌 보조 인덱스)

```
Base Table: Orders
PK: UserId
SK: OrderId

GSI: StatusDateIndex
PK: Status   ← 완전히 다른 Partition Key
SK: OrderDate ← 완전히 다른 Sort Key
```

**특징**:
- **완전히 다른 Partition Key + Sort Key** 설정 가능
- **언제든 생성/삭제** 가능 (운영 중에도)
- **테이블당 최대 20개**
- **Eventually Consistent Read만** 가능 (Strongly consistent X)
- **별도 Throughput 설정** (RCU/WCU) → 추가 비용
- Base 테이블과 **독립된 저장소**

**사용 사례**:
- "오늘 상태가 PENDING인 모든 주문" (`PK=PENDING, SK=today`)
- "특정 상품의 전체 주문" (`PK=productId, SK=orderId`)

### LSI vs GSI 비교표

| 항목 | **LSI** | **GSI** |
|-----|---------|---------|
| **Partition Key** | 같음 (Base와) | **다름 가능** |
| **Sort Key** | 다름 | 다름 (optional) |
| **생성 시점** | 테이블 생성 시만 | **언제든** |
| **삭제** | ❌ (테이블 삭제해야) | ✅ 언제든 |
| **최대 개수** | **5개** | **20개** |
| **Consistent Read** | Strongly OR Eventually | **Eventually만** |
| **Throughput** | Base와 공유 | **별도** |
| **저장 비용** | Base에 포함 | 별도 |
| **파티션 10 GB 제한** | 포함됨 (⚠️) | 영향 없음 |

### 어느 걸 쓸까?

- **같은 PK로 다른 정렬** 필요 → **LSI**
- **완전히 다른 쿼리 패턴** 필요 → **GSI**
- **운영 중 추가** 필요 → **GSI**
- **Strongly consistent** 필수 → **LSI**
- **대규모 Partition** (10 GB 초과 가능) → **GSI** (LSI는 파티션 제한에 걸림)

실무에선 **GSI를 훨씬 많이 씀** (유연성).

---

## 4부. Query vs Scan ⭐

**데이터 조회 방법 2가지**. 성능 차이 **100~1000x** — 매우 중요.

### Query — Partition Key 필수

```python
# user-123의 주문 중 OrderDate >= '2024-01' 조회
response = table.query(
    KeyConditionExpression='UserId = :u AND OrderDate >= :d',
    ExpressionAttributeValues={':u': 'user-123', ':d': '2024-01'}
)
```

**특징**:
- **Partition Key 조건 필수** (Equal 조건만, 범위 불가)
- Sort Key는 선택 (범위 조건 가능: `=, <, >, BETWEEN, begins_with`)
- **해당 파티션만 접근** → 매우 효율
- **1 MB 결과 제한** (초과 시 LastEvaluatedKey로 pagination)
- `ScanIndexForward=False`로 역순 가능

**과금**: 반환된 데이터 크기 기반 (RCU 소모)

### Scan — 전체 테이블 순회 ⚠️

```python
# 모든 주문 중 Amount > 10000 조회
response = table.scan(
    FilterExpression='Amount > :a',
    ExpressionAttributeValues={':a': 10000}
)
```

**특징**:
- **전체 테이블 순회** — 모든 파티션 접근
- **매우 비효율** (RDBMS의 full table scan과 유사)
- FilterExpression은 **스캔 후 필터** — 전체 데이터 읽은 뒤 걸러냄
- **1 MB 결과 제한** (pagination 필요)

**과금**: **스캔한 전체 데이터**에 대해 RCU 소모 (filter와 무관)

### Query vs Scan 비교

| 항목 | **Query** | **Scan** |
|-----|---------|----------|
| **Partition Key** | 필수 | 불필요 |
| **접근 범위** | 1 Partition | 전체 테이블 |
| **성능** | **매우 빠름** (< 10 ms) | 매우 느림 (초~분) |
| **과금** | 반환 데이터만 | **전체 스캔 데이터** |
| **사용처** | **대부분의 경우** | 긴급 분석, 작은 테이블 |

### Filter vs Condition — 중요한 차이

```python
# ❌ Bad — 전체 스캔 후 Amount 필터 (매우 비싼 쿼리)
table.scan(FilterExpression='Amount > 10000')

# ✅ Good — Partition Key로 좁힌 뒤 Amount 필터
table.query(
    KeyConditionExpression='UserId = :u',
    FilterExpression='Amount > :a',
    ...
)
```

**규칙**:
- **KeyConditionExpression**: Query 전 **인덱스로 조회 범위 좁힘** (효율)
- **FilterExpression**: 조회 후 **애플리케이션 레벨에서 걸러냄** (RCU는 이미 소모됨)

### Scan 어떻게든 써야 한다면

**Parallel Scan**:
```python
# 테이블을 N segment로 쪼개서 병렬 처리
for i in range(4):
    table.scan(TotalSegments=4, Segment=i)
```

→ 처리 속도 N배. 단, Throughput도 N배 소모.

### 실무 원칙

- [ ] **Query 사용 — Scan은 금지에 가깝게**
- [ ] 접근 패턴을 미리 분석해 **PK/SK/GSI 설계**
- [ ] **"어떤 쿼리를 할 것인가"**를 먼저 정하고 테이블 설계
- [ ] Scan이 필요한 분석 쿼리는 **Export to S3 + Athena** 또는 Redshift로

---

## 5부. Capacity Mode (용량 모드)

### Provisioned Capacity

```
RCU: 1000
WCU: 500
```

- **고정 처리량** 미리 할당
- 예측 가능한 트래픽에 유리
- **Auto Scaling** 가능 (사용률 기반)
- 저렴 (예측 맞으면)

**RCU (Read Capacity Unit)**:
- 1 strongly consistent read of **4 KB** / 초
- 1 eventually consistent → 2 reads/초
- 1 transactional → 0.5 reads/초

**WCU (Write Capacity Unit)**:
- 1 write of **1 KB** / 초
- 1 transactional → 0.5 writes/초

### On-Demand Capacity

- **사용량만큼 과금**
- 즉시 확장 (traffic burst 대응)
- 예측 불가/변동 큰 서비스에 유리
- Provisioned보다 **5~7배 비쌈** (사용 많으면)

### 선택 가이드

| 상황 | 추천 |
|-----|-----|
| 트래픽 예측 가능, 상시 운영 | **Provisioned + Auto Scaling** |
| 신규 서비스, 트래픽 불명확 | **On-Demand** (시작) |
| Spike 트래픽 | **On-Demand** |
| 대규모 안정 운영 | **Provisioned** (비용 효율) |

---

## 6부. 그 외 핵심 기능

### DynamoDB Streams

**테이블 변경을 실시간 캡처** → Lambda 트리거 가능.

```
Table Change (INSERT/UPDATE/DELETE)
    ↓
Stream (24시간 유지)
    ↓
Lambda (자동 처리)
    ↓
다른 서비스 (S3, Kinesis, SNS...)
```

**사용 사례**: CDC, 캐시 무효화, 실시간 분석, 다른 DB 복제

### Global Tables

**Multi-Region 자동 복제**:
```
Region A (서울) ⇄ Region B (버지니아) ⇄ Region C (도쿄)
```
- 각 지역에서 **읽기/쓰기** 모두 가능
- Last-Writer-Wins 충돌 해결
- **글로벌 서비스**에 적합

### TTL (Time to Live)

- **자동 만료 삭제**
- Item에 `ExpiresAt` attribute 설정 → 그 시점에 자동 삭제
- 세션/임시 데이터 자동 정리
- **배경 삭제** (몇 분~48시간 지연)
- 과금 없음 (삭제는 무료)

### DAX (DynamoDB Accelerator)

**DynamoDB 전용 In-Memory 캐시**:
- μs 단위 응답
- API 호환 (코드 변경 거의 없음)
- Read 성능 10x+ 향상

### Transactions

**ACID 트랜잭션** 지원 (2018+):
```python
dynamodb.transact_write_items(TransactItems=[
    {'Put': {...}},
    {'Update': {...}},
    {'Delete': {...}},
])
```

- 최대 **100 item** per transaction
- 전부 성공 or 전부 실패

### Backup

- **On-Demand Backup** — 수동
- **PITR (Point-in-Time Recovery)** — 35일 내 임의 시점
- 운영 중단 X

---

## 7부. 실무 설계 패턴

### Single Table Design

**여러 종류의 데이터를 한 테이블에**. DynamoDB 모범 사례.

```
PK (User-X)      SK                    Type         Attributes
user-123        PROFILE                user         {name, email}
user-123        ORDER#2024-01-01       order        {amount, items}
user-123        ORDER#2024-01-15       order        {amount, items}
user-123        ADDRESS#home           address      {street, city}
```

→ 한 번의 Query로 user-123의 **모든 데이터** 조회 가능.

### 접근 패턴 중심 설계

**RDBMS**: 데이터 중심 (정규화) → 쿼리에 맞춰
**DynamoDB**: **쿼리 중심** (비정규화) → 데이터 복제 각오

```
① 어떤 쿼리를 할 것인가 나열
   - "user의 최근 주문 10개"
   - "오늘 PENDING 주문 전체"
   - "상품별 주문 리스트"

② 각 쿼리에 맞춰 PK/SK/GSI 설계
   - Query 1: PK=UserId, SK=OrderDate (base)
   - Query 2: GSI PK=Status, SK=OrderDate
   - Query 3: GSI PK=ProductId, SK=OrderId
```

### 안티패턴

- [ ] ❌ Scan 사용 (대부분 불필요)
- [ ] ❌ 복잡한 JOIN (DynamoDB엔 없음)
- [ ] ❌ 모든 attribute에 index (비용 폭증)
- [ ] ❌ Hot Partition Key (ALL_USERS, Date 등)
- [ ] ❌ 큰 item 저장 (최대 400 KB)
- [ ] ❌ 트랜잭션 과용 (2x 비용)

---

## 참고 자료

- [DynamoDB Developer Guide](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/)
- [DynamoDB Best Practices](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)
- [Single Table Design (Alex DeBrie)](https://www.alexdebrie.com/posts/dynamodb-single-table/)
- [Dynamo Paper (2007)](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf)
