### Story 24. 애플리케이션에서 점검해야 할 대상들

> 책 전체를 종합한 **점검 체크리스트**. 각 항목은 상세 설명 Story로 crosslink.

---

## 1부. 코드 레벨 점검

#### 문자열 처리 ([Story 03](../story03/README.md))
- [ ] 반복문 안에서 `String +` 연산 → `StringBuilder`로 교체
- [ ] 멀티 스레드 공유 아니면 `StringBuffer` 대신 `StringBuilder`
- [ ] JDK 9+ compact strings 활용 (별도 작업 없이 자동)
- [ ] 긴 String concat은 `StringBuilder(expectedSize)`로 초기 용량 지정

#### Collection 선택 ([Story 04](../story04/README.md))
- [ ] 랜덤 접근 많으면 `ArrayList`, 중간 삽입 많으면 `LinkedList` 아니라 성능 재검토 (대부분 `ArrayList`)
- [ ] 멤버십 체크 많으면 `List.contains` 대신 `HashSet`
- [ ] 멀티 스레드 Map은 `HashMap` 대신 `ConcurrentHashMap`
- [ ] 초기 용량 추정 가능하면 생성 시 지정 (resize 비용 제거)
- [ ] mutable 객체를 Map key로 사용 금지 (hashCode 변경 시 조회 실패)
- [ ] `hashCode()`와 `equals()`는 **반드시 같이** override

#### 반복문 최적화 ([Story 05](../story05/README.md))
- [ ] 루프 안에서 매번 `collection.toArray()`, `Pattern.compile()` 등 호출 금지 → 루프 밖으로
- [ ] `for (int i = 0; i < list.size(); i++)` 패턴에서 `size()`가 비싼 메서드면 변수에 담기
- [ ] `arr.length`는 field 접근이라 안전, `collection.size()`는 메서드 호출
- [ ] switch는 case 값이 밀집할 때 tableswitch로 변환 (연속 숫자 선호)

#### static 사용 ([Story 06](../story06/README.md))
- [ ] 상수는 `public static final`
- [ ] `static final` 컬렉션은 `Collections.unmodifiableXxx` or `List.of()` 등 immutable 보장
- [ ] mutable static 컬렉션은 **메모리 leak 위험**
- [ ] 자주 쓰는 `Pattern`, `DateTimeFormatter`는 static final로 재사용
- [ ] `static ThreadLocal`은 반드시 `remove()` finally 블록에

#### Reflection 과용 ([Story 07](../story07/README.md))
- [ ] 반복 호출 경로에서 `getDeclaredMethod()` lookup 금지 → 캐싱
- [ ] `Method.invoke()` 대신 가능하면 `MethodHandle` 고려 (JDK 7+)
- [ ] JDK 17+ 모듈 캡슐화 대응 (`setAccessible` + `--add-opens`)

#### Thread/Synchronization ([Story 08](../story08/README.md))
- [ ] `synchronized` 블록 범위 **최소화**
- [ ] `static synchronized`는 전역 병목 → 신중히 사용
- [ ] `Thread.sleep()`을 `synchronized` 안에서 호출 금지 (lock 보유 중)
- [ ] `wait()`는 `while` 체크 (spurious wakeup)
- [ ] `interrupt()`로 멈추려면 `isInterrupted()` 체크 로직 포함
- [ ] 가능하면 `java.util.concurrent` 도구 사용 (Atomic, Lock, Concurrent collections)
- [ ] 멀티 스레드 counter는 `AtomicLong` 또는 **고경합이면 `LongAdder`**

#### IO 처리 ([Story 09](../story09/README.md))
- [ ] 모든 Stream/Reader/Connection은 **try-with-resources**
- [ ] 작은 I/O 반복은 `BufferedInputStream`/`BufferedReader` 로 감싸기
- [ ] 대용량 파일 복사는 `FileChannel.transferTo` (zero-copy)
- [ ] 성능 민감한 I/O는 NIO + DirectBuffer 고려

#### 로깅 ([Story 10](../story10/README.md))
- [ ] `System.out.println` 금지 → Logger
- [ ] `log.debug("x=" + x)` → `log.debug("x={}", x)` (지연 평가)
- [ ] 비싼 메서드 호출 포함 로그는 `isDebugEnabled()` 체크 or Supplier
- [ ] 민감 정보(비밀번호/카드/주민번호) 로그 출력 금지
- [ ] Request ID / Trace ID를 MDC로 모든 로그에 포함
- [ ] 운영 로그 레벨은 INFO 이상 (DEBUG 상시 금지)
- [ ] Rolling 설정 (파일 크기, 보관 기간)

#### Exception 사용
- [ ] Exception을 flow control로 쓰지 말 것 (성능 저하 심각)
- [ ] 자주 발생하는 예외는 `fillInStackTrace()` override 고려
- [ ] 로그에 예외 삼키기(swallow) 금지 — 반드시 stack trace 포함

---

## 2부. JVM 레벨 점검

#### 메모리 (Heap/Metaspace) ([Story 06](../story06/README.md), [Story 18](../story18/README.md))
- [ ] `-Xms = -Xmx` 설정 (resize 비용 제거)
- [ ] `-XX:MaxMetaspaceSize` **반드시 명시** (기본 unlimited)
- [ ] `-XX:+HeapDumpOnOutOfMemoryError` 활성화
- [ ] `-XX:HeapDumpPath` 지정 (OOM dump 위치)
- [ ] `-XX:+ExitOnOutOfMemoryError` 활성화 (orchestrator가 재시작)

#### GC ([Story 06](../story06/README.md), [Story 19](../story19/README.md))
- [ ] GC 알고리즘 선택 명시 (`-XX:+UseG1GC` 등)
- [ ] 저지연 서비스면 `-XX:+UseZGC -XX:+ZGenerational` 검토 (JDK 21+)
- [ ] `-Xlog:gc*=info:file=gc.log` 상시 로그 수집
- [ ] Full GC 분당 횟수 모니터링 (분당 1회 이상 경보)
- [ ] GC overhead < 5% 유지
- [ ] `System.gc()` 호출 금지 (코드에서) + `-XX:+DisableExplicitGC`
- [ ] G1이면 `-XX:MaxGCPauseMillis` 설정 (기본 200)

#### JIT ([Story 16](../story16/README.md))
- [ ] Tiered Compilation 기본 ON 확인 (JDK 7+ 기본)
- [ ] Code Cache 사용률 체크 (가득 차면 JIT 멈춤)
- [ ] 짧은 수명 애플리케이션에는 AOT or Native Image 고려

#### Thread ([Story 14](../story14/README.md))
- [ ] Thread stack `-Xss` 설정 (기본 1 MB — Virtual Thread 사용 시 영향 작음)
- [ ] JDK 21+는 Virtual Thread 검토 (`Thread.ofVirtual()`, `Executors.newVirtualThreadPerTaskExecutor()`)

---

## 3부. 애플리케이션 레벨 점검

#### WAS 설정 ([Story 14](../story14/README.md))
- [ ] `server.tomcat.threads.max` 명시 (기본 200)
- [ ] `server.tomcat.max-connections` 명시 (기본 8192)
- [ ] `server.tomcat.accept-count` 명시 (기본 100)
- [ ] Thread 수 : DB Connection 수 비율 적절 (일반 5:1 ~ 20:1)
- [ ] Graceful shutdown 활성화 (`server.shutdown=graceful`)

#### DB Connection Pool ([Story 12](../story12/README.md))
- [ ] HikariCP 사용 (Spring Boot 2+ 기본)
- [ ] `maximum-pool-size` 명시 (기본 10)
- [ ] `connection-timeout` 모니터링 (타임아웃 발생 시 alert)
- [ ] `max-lifetime` < DB의 `wait_timeout` (DB가 먼저 끊으면 broken connection)
- [ ] `leak-detection-threshold` 설정 (개발/스테이징 2초)
- [ ] 트랜잭션 안에서 외부 API 호출 금지 ⭐
- [ ] 긴 쿼리는 read replica 분리
- [ ] 배치 작업용 별도 DataSource 분리

#### Spring 특이사항 ([Story 11](../story11/README.md))
- [ ] `@Transactional`은 public 메서드에만 (proxy 한계)
- [ ] self-invocation 함정 주의 (같은 클래스 내 호출은 proxy 안 거침)
- [ ] `@Transactional(rollbackFor = Exception.class)` 명시 (checked exception도 rollback)
- [ ] Lazy loading 사용 시 트랜잭션 범위 확인 (LazyInitializationException)
- [ ] `@Async` + `@Transactional` 조합 순서 확인
- [ ] `@Scheduled`로 반복 실행되는 작업은 별도 DataSource 사용

---

## 4부. 인프라 레벨 점검

#### 리소스
- [ ] CPU 사용률 (평균 60% 이하, 피크 80% 이하)
- [ ] Memory 사용률 (container limit 대비, GC overhead 포함)
- [ ] Disk I/O (iops, throughput, latency)
- [ ] Network latency (내부 서비스 간 호출 지연)
- [ ] File descriptor 수 (`ulimit -n` + 실제 사용량)

#### Container (Kubernetes/Docker)
- [ ] JVM이 container memory limit 인식 (`-XX:+UseContainerSupport` 기본 ON JDK 10+)
- [ ] Memory request/limit 합리적 설정 (OOMKilled 방지)
- [ ] `livenessProbe.timeoutSeconds` > GC pause time
- [ ] `readinessProbe`는 health check와 분리 (초기화 중 트래픽 받지 않도록)
- [ ] Graceful shutdown + `terminationGracePeriodSeconds` 조정

---

## 5부. DB 점검

- [ ] Slow query log 활성화 + 알림
- [ ] N+1 쿼리 발견 (JPA/Hibernate — `@EntityGraph`, `JOIN FETCH`로 해결)
- [ ] Index 사용 여부 (`EXPLAIN` 분석)
- [ ] DB 세션 수 vs `max_connections` 비율
- [ ] 트랜잭션 격리 수준 (대부분 READ COMMITTED로 충분)
- [ ] Dead lock 로그 모니터링
- [ ] `wait_timeout` > HikariCP `max-lifetime`
- [ ] Read replica 활용 (SELECT 분리)

---

## 6부. 모니터링 / 관측성 ([Story 20](../story20/README.md))

- [ ] Health check endpoint 노출 (`/actuator/health`)
- [ ] Metrics endpoint 노출 (`/actuator/prometheus`)
- [ ] Trace ID 로그 포함 (분산 추적)
- [ ] APM 또는 OpenTelemetry 설치
- [ ] Grafana 대시보드:
  - 응답시간 p50/p95/p99
  - 처리량 (TPS, RPS)
  - Error rate
  - JVM: heap, non-heap, GC, thread
  - DB connection pool: active/idle/waiting
  - Tomcat thread: busy/max
- [ ] Alert 설정:
  - 응답시간 p99 > 임계값
  - Error rate > 1%
  - Full GC 분당 1회 이상
  - Heap 사용률 > 85%
  - DB connection pending > 0 지속

---

## 7부. 보안 점검

- [ ] 민감 정보 (password, token, 주민번호, 카드번호) 로그 금지
- [ ] 외부 입력 검증 (`@Valid`, Bean Validation)
- [ ] SQL Injection 방지 (`PreparedStatement`, ORM 사용)
- [ ] 의존성 CVE 스캔 (Dependabot, Snyk, OWASP Dependency Check)
- [ ] Log4j 1.x 사용 금지 (CVE 다수, EOL)
- [ ] Log4j 2는 **2.17.1+** 사용 (Log4Shell CVE-2021-44228)
- [ ] Secret은 코드/git에 커밋 금지 (환경변수, Vault, K8s secret)
- [ ] HTTPS/TLS 강제
- [ ] CORS 설정 명시 (와일드카드 금지)
- [ ] Spring Boot Actuator endpoint 접근 제어 (`/env`, `/heapdump` 등 공개 금지)

---

## 8부. 운영 점검

- [ ] Graceful shutdown 동작 확인 (K8s SIGTERM → 요청 drain)
- [ ] Health check 분리 (readiness vs liveness)
- [ ] Log level 운영 중 변경 가능 (`/actuator/loggers` POST)
- [ ] Heap dump 자동 수집 (OOM 시) + 저장소 확보
- [ ] GC log 상시 수집 + 보관 (최소 7일)
- [ ] JFR 상시 수집 (저오버헤드 — JDK 11+ 무료)
- [ ] 배포는 blue-green or canary (롤백 플랜 포함)
- [ ] 롤백 플랜: 설정 복원, DB 마이그레이션 역방향, 이전 버전 이미지 유지

---

## 9부. 외부 통합 점검

- [ ] HTTP 클라이언트 timeout 명시 (connect + read + write)
- [ ] HTTP 클라이언트 connection pool 설정 (`HttpClient.newBuilder()` 또는 Apache/OkHttp)
- [ ] Retry 정책 (지수 백오프 + jitter)
- [ ] Circuit Breaker (Resilience4j, Hystrix 등)
- [ ] Timeout 체인: client < DB < 외부 API — 가장 바깥이 가장 길어야 함
- [ ] 외부 API 호출은 트랜잭션 밖에서

---

## 10부. 성능 테스트

- [ ] 부하 테스트 자동화 (JMeter, k6, Gatling)
- [ ] 매 배포 전 regression 테스트
- [ ] 목표 TPS/latency 기준 명문화 (SLA)
- [ ] 트래픽 패턴 재현 (평균 vs 피크)
- [ ] Stress test (한계 측정) + Soak test (장기간)
- [ ] 메모리 leak 검증 (24시간 테스트 후 heap 안정)

---

## 책 전체 → 현대적 재해석

[이상민 — 자바 성능 튜닝 이야기 (2008, 2013 개정)]를 2026 관점에서 종합:

**여전히 유효**:
- 코드 레벨 최적화 원칙 (문자열, 컬렉션, 반복문, static)
- JVM 메모리/GC 기초 개념
- 모니터링/튜닝 **철학**

**현대적으로 재해석**:
- GC 알고리즘 → **ZGC/Shenandoah/G1 Generational ZGC** (2008엔 CMS/Parallel)
- JMX → **Micrometer + Prometheus + OpenTelemetry**
- Thread → **Virtual Thread** (JDK 21+)
- Reflection → **MethodHandle, VarHandle** (JDK 7+/9+)
- Spring 생태계 → **Spring Boot + Actuator**
- 배포 → **Kubernetes + container-aware JVM**

**이 책의 가치**:
- 성능 튜닝의 **방법론**은 2008이나 2026이나 동일
- 측정 → 가설 → 적용 → 검증 → 반복
- **"튜닝은 최후의 수단, 코드가 먼저"** (Story 19의 핵심)

---

## 참고 자료

- Story 03 ~ 20 각 상세
- "Java Performance" by Scott Oaks (2020, 2nd Ed) — 현대적 Java 성능 서적
- "Optimizing Java" by Benjamin J. Evans — JIT/GC 심층
- "Systems Performance" by Brendan Gregg — OS 레벨 성능
