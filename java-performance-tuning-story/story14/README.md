### Story 14. 서버를 어떻게 세팅해야 할까?

## 1부. Spring Boot + Tomcat 기본 설정값

#### Embedded Tomcat (verified Spring Boot 3.2.5 소스)

| 설정 | 기본값 | 의미 |
|-----|------|------|
| `server.tomcat.threads.max` | **200** | 요청 처리 **최대 worker 스레드 수** |
| `server.tomcat.threads.min-spare` | **10** | 유지할 최소 worker 수 |
| `server.tomcat.max-connections` | **8192** | 수락 가능한 최대 TCP 연결 수 |
| `server.tomcat.accept-count` | **100** | 모든 thread가 바쁠 때 OS 백로그 큐 크기 |
| `server.tomcat.connection-timeout` | **20000 ms** | keep-alive 유휴 timeout |

**스레드 vs 연결 vs 큐 관계**:
```
Client ─▶ ① TCP 연결 (max-connections = 8192 까지 수락)
            │
            └─▶ ② Thread Pool 할당 (max threads = 200)
                     │
                     ├─ 사용 가능한 thread 있음 → 처리 시작
                     │
                     └─ 전부 사용 중 → accept-count=100 큐에 대기
                                        │
                                        └─ 큐도 꽉 참 → ⚠️ 연결 거부
```

→ 200 요청까지 **동시 처리**, 100개는 **큐에서 대기**, 나머지는 **거부**.

#### HikariCP (verified HikariCP 5.1.0 소스)

| 설정 | 기본값 | 의미 |
|------|------|------|
| `spring.datasource.hikari.maximum-pool-size` | **10** | 보유 가능한 최대 connection 수 |
| `spring.datasource.hikari.minimum-idle` | `maximum-pool-size`와 동일 | 유지할 최소 유휴 connection |
| `spring.datasource.hikari.connection-timeout` | **30000 ms** | `getConnection()` 대기 최대 시간 |
| `spring.datasource.hikari.idle-timeout` | **600000 ms** (10 min) | 유휴 connection을 유지할 시간 |
| `spring.datasource.hikari.max-lifetime` | **1800000 ms** (30 min) | connection 하나의 수명 |

→ **Tomcat 기본 200 thread** vs **Hikari 기본 10 connection**. 20:1 비율. 이 격차가 중요한 포인트 (아래 3부).

#### JVM 기본값 (JDK 21, 서버 모드)

| 설정 | 기본값 |
|-----|------|
| 기본 GC | **G1** (ergonomic) |
| 초기 heap (`-Xms`) | 물리 RAM의 1/64 (최소 8 MB) |
| 최대 heap (`-Xmx`) | 물리 RAM의 **1/4** (최대 32 GB) |
| Thread stack (`-Xss`) | 1 MB |
| Metaspace | 상한 없음 (OS 메모리까지) |

---

## 2부. STW GC 발생 시 어떤 일이 생기나

**Stop-The-World (STW)**: GC 작업 중 **모든 애플리케이션 스레드가 일시 정지**됨.

#### 발생 시 상황 체인

```
① GC 시작 → safepoint 도달 → ALL 스레드 정지
    │
    ├─ 요청 처리 중이던 Tomcat worker 200개 전부 멈춤
    ├─ DB connection 보유한 채 멈춤 → DB 쪽은 "응답 없는 client"로 인식
    ├─ 외부 health check (/health) 응답 못 함
    └─ Keep-alive 유지 중인 client도 응답 지연
    │
② GC 진행 (예: Full GC 수십 초)
    │
③ GC 완료 → 전체 재개
    │
    ├─ 요청 큐 폭증 (대기한 요청들이 몰림)
    ├─ 외부 timeout 발생 (client가 이미 포기한 요청에도 응답)
    └─ DB connection timeout (connection 획득 못 함)
```

#### Cascading Failure 시나리오 (운영 사고 실제 패턴)

```
0초: Full GC 시작 (3초 소요)
3초: GC 완료, 재개
     │
     ├─ 대기 요청 500개 몰림 → thread 200개로 처리 시작
     ├─ DB connection 10개 → 490개 요청이 getConnection 대기
     ├─ 20초 뒤: Hikari connectionTimeout(30초) 임박
     │
10초: LB health check 실패 → 트래픽 재분배
     │
     └─ 다른 인스턴스로 몰림 → 그 인스턴스도 과부하 → GC → ...
```

→ **1대의 GC가 전체 cluster로 전염**. 가장 흔한 장애 패턴.

#### GC별 STW 시간 (일반적 수치)

| GC | 최대 STW | heap 1GB당 STW | 특징 |
|----|---------|---------------|------|
| **Serial** | 매우 긺 (수십 초) | ~1초 | 작은 heap, CLI 용 |
| **Parallel** | 긺 (수 초~수십 초) | ~0.5초 | throughput 중심 |
| **G1** | 조절 가능 (`MaxGCPauseMillis`) | 수십~수백 ms | 범용 (기본) |
| **ZGC** | **< 10 ms** (JDK 15+) | heap 크기 무관 | 저지연 서비스 |
| **Shenandoah** | < 10 ms | heap 크기 무관 | ZGC 대안 |

**실무 영향**:
- 응답시간 p99 민감 (예: 금융, 광고 입찰) → **ZGC** 고려
- 일반 웹 서비스 → G1 + MaxGCPauseMillis=200 으로 충분
- Full GC 횟수가 지표. **분당 1회 이상이면 heap 부족 or leak** 의심.

#### STW 방어 체크리스트

- [ ] GC 로그 수집 (`-Xlog:gc*=info:file=gc.log:tags,uptime,time:filecount=10,filesize=10m`)
- [ ] Full GC 발생 시 알림 (분당 횟수)
- [ ] Health check timeout을 **GC pause보다 크게** 설정 (안 그러면 GC로 재시작 반복)
- [ ] Kubernetes `livenessProbe.timeoutSeconds`를 넉넉히
- [ ] Heap 적정 설정 — 너무 크면 Full GC 시간 길어짐, 너무 작으면 GC 자주

---

## 3부. WAS Thread vs DB Connection — 연관 관계

#### Request 처리 모델 (전통적 blocking)

```
Client Request
     │
     ▼
Tomcat Worker Thread 1개 할당
     │
     ├─ 1. request parsing (CPU)
     ├─ 2. Controller 실행 (CPU)
     ├─ 3. @Transactional 시작
     │     └─ dataSource.getConnection()  ← DB Pool에서 1개 획득 ★
     ├─ 4. Service / Repository 실행
     │     └─ DB 쿼리 (connection 보유 중)
     ├─ 5. @Transactional 종료
     │     └─ connection 반환 ★
     ├─ 6. 응답 생성 (CPU)
     └─ 7. HTTP response 전송 (IO)
```

→ **Thread는 요청 처리 끝까지 보유**, **Connection은 트랜잭션 구간만 보유**.

#### 왜 Thread > Connection? — 4가지 이유 ⭐

**이유 1 — 모든 요청이 DB를 쓰진 않는다**

```java
@GetMapping("/health")       // DB 안 씀
public String health() { return "OK"; }

@GetMapping("/static/*")      // 정적 리소스, DB 안 씀
public Resource serve(...) { ... }

@GetMapping("/user/{id}")     // 캐시 hit이면 DB 안 씀
public User get(@PathVariable Long id) {
    return cache.getOrLoad(id, () -> repo.find(id));
}
```

→ 수백 req/s 중 **DB 접근은 그 중 일부**만.

**이유 2 — Thread가 Connection을 보유하는 시간은 전체 요청 시간의 일부**

```
요청 처리 100 ms 중:
├─ 20 ms: request parsing + 응답 serialize (DB X)
├─ 10 ms: 비즈니스 로직 계산 (DB X)
├─ 50 ms: DB 쿼리 (connection 보유 ★)
└─ 20 ms: 응답 전송 (DB X)
```

- Thread 보유 시간 = **100 ms**
- Connection 보유 시간 = **50 ms**
- Connection은 **50% 시간만 쓰임** → 2 thread가 1 connection 공유 가능

**이유 3 — Little's Law로 정량화**

```
L = λ × W
  L: 동시 처리 중인 요청 수 (concurrent requests)
  λ: 처리율 (req/s)
  W: 평균 처리 시간 (s)
```

**Thread 수 산정**:
```
목표: 1000 req/s, 평균 응답 100 ms
→ L = 1000 × 0.1 = 100 concurrent
→ Thread 최소 100개 필요
```

**Connection 수 산정**:
```
그 중 DB 사용 비율: 50 ms / 100 ms = 50%
→ Connection = 100 × 50% = 50개
```

→ **Thread 100 : Connection 50 = 2:1**.

실제 상용 서비스는 보통 **Thread : Connection = 10:1 ~ 30:1** 수준.

**이유 4 — 백프레셔(Backpressure) 흡수**

```
Pool 고갈 상황:
   Connection 10개 전부 사용 중
   추가 요청 20개 도착
```

Thread > Connection 이면:
- 20개 요청이 각각 thread를 받아서 **getConnection 대기** 상태로 들어감
- Connection 반환되면 즉시 처리 가능 → latency 최소화

Thread ≤ Connection 이면:
- 20개 요청이 thread 받기 전에 Tomcat queue (accept-count=100)에 대기
- Thread 획득도 대기, connection 획득도 대기 → **2중 대기** → latency 2배

#### 비율의 실무 가이드

| 서비스 성격 | Thread : Connection |
|-----------|---------------------|
| 매우 heavy DB (ETL, 리포팅) | 2 : 1 |
| 일반 CRUD 서비스 | 5 : 1 ~ 10 : 1 |
| 캐시 활용 많은 서비스 | 20 : 1 ~ 30 : 1 |
| static/CDN 많은 서비스 | 50 : 1 이상 |

**극단적 비율의 문제**:
- Thread << Connection → Connection은 많은데 쓸 Thread가 없음. 메모리 낭비 + DB 세션 낭비
- Thread >> Connection (너무 큰 비율) → 요청은 받지만 Connection 대기만 함. 응답 느려지고 timeout

---

## 4부. 적정 수치 선정 기준 — CPU, Memory 연관

#### CPU-bound vs IO-bound

**CPU-bound** (이미지 처리, 복잡한 계산, 암호화):
- Thread 수가 많아도 **CPU는 하나씩만 처리**
- 많은 thread → context switch 오버헤드 → **역효과**
- 추천: **Thread 수 = CPU core × 1 ~ 2**

**IO-bound** (DB, HTTP, 파일 — 대부분의 웹 서비스):
- Thread는 대부분 WAIT 상태
- Thread를 많이 두면 **IO 대기 중 다른 thread가 CPU 활용**
- 추천: **Thread 수 = CPU core × 10 ~ 100** (경험적)

#### Thread 수 공식

```
threads = core_count × ((io_wait_time + cpu_time) / cpu_time)
```

예:
- 8 core
- 요청 평균: CPU 10 ms, DB IO 90 ms (총 100 ms)
- → `8 × ((90 + 10) / 10) = 8 × 10 = 80` thread

Tomcat 기본 200은 **대부분의 웹 서비스에서 충분~넉넉**. CPU core 수 적은 서버에선 오히려 과다.

#### Memory 영향

**Thread stack**:
- 기본 1 MB (`-Xss1m`)
- 200 thread = **200 MB stack 메모리**
- Container 메모리 2 GB 중 200 MB가 stack만 차지하는 셈
- Virtual Thread (JDK 21): KB 단위 → 메모리 제약 해소

**Heap 설정과 상관관계**:
- 서버 메모리 = Heap + Metaspace + Thread stacks + Direct memory + OS buffer
- 전형적 배분 (4 GB 컨테이너):
  - Heap: 2 GB
  - Metaspace: 256 MB
  - Thread stacks: 300 MB (300 threads)
  - Direct memory: 256 MB
  - 여유 + OS: 1 GB
- Heap을 너무 크게 잡으면 Full GC pause 증가

**DB Connection의 서버 메모리 영향**:
- Application 서버 자체: connection당 수백 KB (buffer, prepared statement cache)
- **DB 서버**: connection당 더 큼. PostgreSQL은 **connection당 ~10 MB**, MySQL은 ~250 KB
- 서버 10대 × 20 connection = **200 session**
  - PostgreSQL: 2 GB DB memory
  - MySQL: 50 MB

→ DB의 `max_connections`와 비교. 너무 많으면 DB 메모리 고갈 → DB 자체가 느려짐.

#### Connection Pool 공식 (HikariCP 가이드)

```
connections = ((core_count × 2) + effective_spindle_count)
```

예: 8 core + SSD(spindle=1) → `(8×2) + 1 = 17`

**이게 Tomcat 기본 200과 왜 다른가?** — DB의 **동시 쿼리 처리 능력** 때문. DB는 CPU 코어 수 이상의 동시 쿼리를 처리해도 **디스크 IO 경합** 발생 → 적게 유지하는 게 전체 처리량에 유리.

#### 종합 산정 예시

**시나리오**: 쇼핑몰 API 서버, 8 core, 4 GB 메모리, 1000 req/s 목표

```
① 요청 특성 분석:
   - 평균 응답 100 ms
   - 그 중 DB IO 40 ms (40%)
   - 캐시 hit율 50% (연쇄 분석 필요)

② Little's Law로 Thread 산정:
   L = 1000 × 0.1 = 100 concurrent requests
   → Thread 수: 100 ~ 150 (여유 50%)
   → Tomcat max-threads = 150

③ Connection 산정:
   50% × 100 concurrent × 40% DB time = 20 (DB 사용 비율 20%)
   → maximumPoolSize = 20

④ Memory 체크:
   - Thread stack: 150 × 1 MB = 150 MB ✅
   - Heap: 2 GB
   - Metaspace: 300 MB
   - Direct: 256 MB
   - 여유: ~1.2 GB ✅

⑤ Thread : Connection = 150 : 20 = 7.5 : 1 ✅ (적절)
```

---

## 5부. 실무 체크리스트

**설정 검증**:
- [ ] `server.tomcat.threads.max` 명시 (기본 200 그대로 쓰지 말 것)
- [ ] `spring.datasource.hikari.maximum-pool-size` 명시 (기본 10)
- [ ] `max-lifetime` < DB의 `wait_timeout` (DB가 먼저 끊으면 broken connection)
- [ ] JVM: `-Xms`와 `-Xmx` 같게 (resize 비용 제거)
- [ ] JVM: `-XX:MaxGCPauseMillis` 설정 (G1)

**부하 테스트 전 질문**:
- 목표 TPS는?
- 평균 응답시간은?
- DB 쿼리 비중은?
- Little's Law로 Thread/Connection 산정했나?

**모니터링 지표**:
- Tomcat `busy threads` / `max threads` 비율
- Hikari `active connections` / `max pool size` 비율
- GC: Full GC 횟수 분당, GC 시간 비율
- DB: `pg_stat_activity` (active sessions) / `max_connections`

---

## 참고 자료

- Spring Boot `ServerProperties.java` (3.2.5 verified)
- HikariCP `HikariConfig.java` (5.1.0 verified)
- HikariCP Pool Sizing — [About Pool Sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing)
- Little's Law — queueing theory 기초
- Tomcat Connector attributes — [tomcat.apache.org](https://tomcat.apache.org/tomcat-10.1-doc/config/http.html)
