### Story 12. DB를 사용하면서 발생 가능한 문제점들

## 1부. DB Connection은 왜 비싼가 — Pool이 필요한 이유

**DB connection 생성 과정**:
1. **TCP 3-way handshake** — 네트워크 왕복 (수 ms)
2. **TLS handshake** (SSL 사용 시) — 추가 RTT 1~2회
3. **Authentication** — username/password 검증, DB 서버 내부 session 생성
4. **Session initialization** — character set, timezone, autocommit 설정 등

**실측 (일반적 수치)**:
- 로컬 PostgreSQL: 20~50 ms
- 네트워크 통한 RDS: 100~300 ms
- SSL 활성화 + 원격: 500 ms 이상

**매 요청마다 새 connection** 만들면:
- API 응답 시간 수백 ms 증가
- DB 서버의 session 리소스 소진 (일반적으로 100~500 max)
- 짧은 수명 TCP connection으로 TIME_WAIT 축적

→ **Connection Pool**: 미리 만들어둔 connection을 재사용. "매번 집 짓는 대신 **호텔 체크인/체크아웃**".

---

## 2부. Connection Pool 개념

#### 핵심 역할

```
┌─────────┐    getConnection()     ┌─────────────────┐
│         │ ────────────────────▶  │ Connection Pool │
│   App   │                        │                 │
│         │ ◀────────────────────  │  [C1 idle]      │
│         │    (대여)              │  [C2 active]    │
│         │                        │  [C3 idle]      │
│         │    close()             │  [C4 idle]      │
│         │ ────────────────────▶  │  [C5 active]    │
└─────────┘    (반환)              └─────────────────┘
                                           │
                                           │ 미리 만든
                                           ▼
                                       ┌──────┐
                                       │  DB  │
                                       └──────┘
```

**Pool이 미리 보유**: TCP 연결 + 인증 완료 + session 초기화된 connection 다수.
**App이 "`close()`"**: 실제론 **TCP 끊지 않고 pool로 반환**. 재사용 대기.

#### 주요 파라미터

| 파라미터 | 의미 | 전형적 값 |
|---------|------|---------|
| `maximumPoolSize` | **동시에 보유 가능한 최대 connection 수** | 10~20 (web app) |
| `minimumIdle` | 유휴로 유지할 최소 수 | `maximumPoolSize`와 같게 (HikariCP 권장) |
| `connectionTimeout` | `getConnection()` 대기 최대 시간 | 30초 (기본) |
| `idleTimeout` | 유휴 connection을 유지할 시간 | 10분 |
| `maxLifetime` | connection 하나의 최대 수명 | 30분 (DB의 timeout보다 짧게) |
| `validationTimeout` | validation 쿼리 timeout | 5초 |
| `leakDetectionThreshold` | leak 의심 로그 임계 | 0 (끔) or 2초 |

#### 유명 구현체

| 라이브러리 | 특징 | 현재 상태 |
|-----------|------|---------|
| **HikariCP** | **Spring Boot 2+ 기본**. 매우 빠름 (bytecode 최적화). FastList, ConcurrentBag 등 lock-free 자료구조 | 사실상 표준 |
| **Tomcat JDBC** | Tomcat 번들. 중간 성능 | 레거시 (유지는 됨) |
| **Apache DBCP2** | commons. 기능 많지만 느림 | 레거시 |
| **c3p0** | 과거 Hibernate 기본. 느리고 설정 복잡 | ❌ 사용 비권장 |

HikariCP가 사실상 표준인 이유: 성능 (타 구현체 대비 수십 배) + 작은 코드 (~2000줄) + 명확한 설정.

---

## 3부. Query 실행 Flow — `getConnection()`부터 반환까지

#### 전체 흐름

```
① app 코드:
   Connection c = dataSource.getConnection();
   PreparedStatement ps = c.prepareStatement("SELECT ...");
   ResultSet rs = ps.executeQuery();
   // ... rs 처리 ...
   rs.close();
   ps.close();
   c.close();   ← ⚠️ 실제론 pool로 반환, TCP 안 끊음

② pool 내부 상태 전이:
   [getConnection() 호출 시]
      │
      ├─ idle connection 있음? ──Yes──▶ 하나 꺼내서 active로 전환 → 반환
      │                                 (validation 설정이면 "SELECT 1" 확인)
      │
      └─ No ──▶ 현재 total < max? ──Yes──▶ 새 connection 생성 → active → 반환
                     │
                     └─ No ──▶ connectionTimeout 동안 대기
                                ├─ 그 사이 반환됨 → 획득
                                └─ timeout → SQLTransientConnectionException

   [c.close() 호출 시]
      │
      ├─ connection 상태 확인 (dirty 트랜잭션 있나?)
      ├─ rollback (auto commit 아닌데 commit 안 한 경우)
      ├─ reset: autoCommit=true, isolation 복구, warnings clear
      └─ idle 큐로 반환
```

#### Spring + `@Transactional` 통합

```java
@Transactional
public void place(Order o) {
    repo.save(o);              // → connection 사용
    paymentService.charge(o);  // → 같은 connection 사용
}
```

내부 플로우:
```
1. Proxy의 TransactionInterceptor 진입
2. DataSourceTransactionManager.getTransaction()
     → dataSource.getConnection()  ★
     → autoCommit = false
     → TransactionSynchronizationManager가 connection을 ThreadLocal에 바인딩
3. 실제 메서드 실행
     - repo.save(o) 내부에서 getConnection() 호출됨
       → SynchManager가 "이 스레드 bound connection" 리턴 (같은 connection!)
     - paymentService.charge(o)도 마찬가지
4. 정상 종료 → commit()
   예외 → rollback()
5. ThreadLocal에서 unbind → connection.close()
     → 실제론 pool로 반환
```

**핵심**: 한 트랜잭션 안에선 **같은 connection을 재사용** (via ThreadLocal). 그래서 `@Transactional` 메서드가 **긴 시간 보유**하면 pool 압박.

#### `close()`의 실제 의미

```java
// JDK 7+ try-with-resources
try (Connection c = dataSource.getConnection();
     PreparedStatement ps = c.prepareStatement("SELECT ...")) {
    try (ResultSet rs = ps.executeQuery()) {
        // ...
    }
}
```

- `c.close()` → HikariCP의 `ProxyConnection.close()` 호출
- 실제론 `physicalConnection.close()`가 아니라 **pool 큐에 반환**
- 다음 `getConnection()` 호출 시 재사용됨

---

## 4부. Connection Pool 고갈 사례 ⭐

Pool 고갈 = **모든 connection이 사용 중이고, `getConnection()`이 timeout** 발생.

전형적 에러 메시지 (HikariCP):
```
HikariPool-1 - Connection is not available, request timed out after 30000ms.
java.sql.SQLTransientConnectionException
```

#### 사례 1 — **트랜잭션 안에서 외부 API 호출** (가장 흔함) ⭐⭐⭐

```java
@Transactional
public Order place(Long userId) {
    User user = userRepo.find(userId);          // 50 ms
    PaymentResult pr = paymentApi.charge(...);  // ⚠️ 외부 HTTP, 2초 대기
    Order order = orderRepo.save(...);          // 20 ms
    return order;
}
```

**문제**:
- `@Transactional` 때문에 **메서드 전체 동안 connection 보유** (2초 이상)
- 동시 요청 100개면 → 100개 connection 필요
- Pool size 10이면 → 10개 처리, 나머지 90개는 timeout까지 대기 → 터짐

**해결**:
```java
public Order place(Long userId) {
    // 1단계: 짧은 트랜잭션 — 사용자 조회 + 임시 주문 생성
    Order tempOrder = createPending(userId);   // @Transactional 안에서

    // 2단계: 외부 API — 트랜잭션 밖
    PaymentResult pr = paymentApi.charge(...);  // 2초, DB 쓰지 않음

    // 3단계: 짧은 트랜잭션 — 결과 반영
    return finalize(tempOrder.getId(), pr);     // @Transactional 안에서
}
```

#### 사례 2 — **긴 쿼리로 인한 블록**

```java
@Transactional
public List<Report> generateReport() {
    return reportRepo.findComplexAggregation();  // 수십 초 걸리는 쿼리
}
```

- 긴 쿼리가 connection을 수십 초 점유
- 동시에 몇 개만 터져도 pool 고갈

**해결**:
- 쿼리 최적화 (인덱스, 분석)
- read replica로 분리
- 배치용 별도 DataSource (pool 분리)

#### 사례 3 — **Connection Leak**

```java
public List<User> listUsers() {
    Connection c = dataSource.getConnection();  // 획득
    // try-with-resources 안 씀
    PreparedStatement ps = c.prepareStatement("SELECT * FROM users");
    ResultSet rs = ps.executeQuery();
    List<User> list = parseAll(rs);              // ⚠️ 여기서 예외 터지면...
    rs.close();
    ps.close();
    c.close();                                    // ← 예외 시 실행 안 됨
    return list;
}
```

- 예외 시 `c.close()` 안 호출됨 → 영원히 반환 안 됨
- 서버 가동 시간이 늘수록 pool이 서서히 고갈
- Rolling restart로 임시 해결하지만 근본 원인

**해결**: **반드시 try-with-resources**.
```java
try (Connection c = dataSource.getConnection();
     PreparedStatement ps = c.prepareStatement("SELECT * FROM users");
     ResultSet rs = ps.executeQuery()) {
    return parseAll(rs);
}
```

HikariCP는 **leak detection** 지원:
```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 2000   # 2초 이상 보유 시 로그 경고
```

#### 사례 4 — **REQUIRES_NEW 중첩**

```java
@Transactional
public void process(Request r) {
    saveMain(r);
    audit(r);  // → @Transactional(propagation = REQUIRES_NEW)
}

@Transactional(propagation = REQUIRES_NEW)
public void audit(Request r) {
    auditRepo.save(r);
}
```

- `process()`가 connection C1 보유 중
- `audit()` 호출 → 새 트랜잭션 → **또 다른 connection C2** 획득
- **한 요청이 2개 connection 소모**
- 중첩 깊어지면 요청당 N개 소모 → 금방 고갈

**해결**: 꼭 필요한 경우에만 `REQUIRES_NEW` (감사 로그, 외부 알림 등). 가능하면 async로.

#### 사례 5 — **배치와 웹 요청의 pool 공유**

```java
@Scheduled(cron = "0 0 2 * * *")
@Transactional
public void nightlyBatch() {
    // 100만 건 처리, 1시간 소요
    users.forEach(u -> process(u));
}
```

- 야간 배치가 10 connection 중 몇 개를 1시간 점유
- 그 시간대 웹 요청들이 pool 압박

**해결**: 배치용 DataSource를 별도 구성, pool 분리.
```java
@Bean("batchDataSource")
public DataSource batchDataSource() { ... }  // batch 전용, max=5

@Bean("webDataSource")
public DataSource webDataSource() { ... }    // web 전용, max=20
```

#### 사례 6 — **DB에서 lock 걸림 → cascade**

- Transaction A가 row X에 write lock
- Transaction B가 같은 row 수정 시도 → DB에서 대기 (connection 보유한 채)
- 요청 많아지면 모두 X 수정하려 → **DB 레벨 대기열이 쌓이고, 각 요청은 connection 보유** → pool 고갈

**해결**:
- lock timeout 설정 (DB 레벨, `SET lock_timeout` 등)
- 비관적 락 대신 낙관적 락 (version 컬럼)
- hot row 분산

#### 진단 방법

**1) Thread dump**:
```bash
jstack <pid> | grep -A 20 getConnection
```
→ `getConnection()`에서 BLOCKED 상태인 스레드가 많으면 pool 압박 확정.

**2) HikariCP metrics** (Spring Boot Actuator):
```yaml
management.endpoints.web.exposure.include: metrics
```
노출되는 지표:
- `hikaricp.connections.active`: 현재 사용 중
- `hikaricp.connections.idle`: 유휴
- `hikaricp.connections.pending`: 대기 중인 스레드
- `hikaricp.connections.timeout`: 지금까지 timeout 발생 횟수

→ `pending > 0`이면 pool 압박. `timeout` 증가하면 심각.

**3) HikariCP 자체 로깅**:
```yaml
logging.level.com.zaxxer.hikari.pool.HikariPool: DEBUG
```
10초마다 상태 로그:
```
HikariPool-1 - Pool stats (total=10, active=10, idle=0, waiting=5)
```

---

## 5부. 적정 Pool 크기 산정

흔한 오해: "더 많이 = 더 좋다". **틀림**.

#### Hikari 공식 (Oracle 과거 가이드 기반)

```
connections = ((core_count * 2) + effective_spindle_count)
```

예:
- 4 core CPU + SSD(spindle=1): `(4 × 2) + 1 = 9`
- 8 core + NVMe: `(8 × 2) + 1 = 17`

**이유**:
- Connection 수 >> CPU core 수면 **context switch 비용**이 성능 저하
- DB도 더 많은 connection을 처리하는 데 메모리/CPU 소비
- "연결은 많은데 각자 느린" 상태가 됨

#### 실무 조정

| 상황 | 권장 |
|-----|------|
| 짧은 쿼리, 많은 요청 | 공식대로 (~10) |
| 긴 트랜잭션 (외부 API 등) | **원인 해결 먼저**, 그 다음 pool 늘려도 효과 작음 |
| CPU-bound 쿼리 | core 수의 1~2배 |
| IO-bound 쿼리 | 공식 + 약간 여유 |

**극단적 오해**: "maximumPoolSize = 200으로 하면 안전하겠지" → DB에 200 session 열림 → DB 메모리 폭증 → DB가 더 느려짐 → 역효과.

---

## 6부. 모니터링 체크리스트

- [ ] `maximumPoolSize` 설정 (기본 10 그대로 쓰지 말고 명시)
- [ ] `connectionTimeout` 모니터링 (`SQLTransientConnectionException` 로그 count)
- [ ] `maxLifetime` < DB의 `wait_timeout` (DB가 먼저 끊으면 broken connection)
- [ ] `leakDetectionThreshold` 설정 (운영에선 끄거나 길게, 개발/스테이징에선 2초)
- [ ] Active/Idle/Pending 지표를 Grafana 등 대시보드에
- [ ] Alert: pending > 0 이 지속 시 알림
- [ ] `@Transactional` 메서드 안 외부 API 호출 금지 (코드 리뷰 체크)

---

## 참고 자료

- HikariCP 공식 문서 — [github.com/brettwooldridge/HikariCP](https://github.com/brettwooldridge/HikariCP)
- HikariCP Pool Sizing — [About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- Spring Boot DataSource configuration reference
- Oracle "The Fastest Way to Fetch Data from a Database"
