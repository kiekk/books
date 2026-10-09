### Story 20. 모니터링 API인 JMX

## 1부. JMX 개념

#### JMX (Java Management Extensions)란

- **JDK 1.3** optional, **JDK 5** 표준 (JSR 3, JSR 160)
- Java 애플리케이션/JVM 상태를 **원격으로 관리/모니터링**하는 공식 API
- JVM 자체도 내부적으로 사용 (platform MBeans)

**핵심 추상화 — MBean (Managed Bean)**:
- 관리/모니터링 대상 Java 객체
- "attribute (상태 읽기/쓰기) + operation (명령 실행) + notification (이벤트 발행)"

#### 아키텍처 — 3 Tier

```
┌─────────────────────────────────────────────┐
│  Tier 3: Distributed Services               │
│  (RMI, JMXMP connectors — 원격 접근)         │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│  Tier 2: Agent Layer                        │
│  MBeanServer (MBean 저장소/레지스트리)       │
└───────────────────┬─────────────────────────┘
                    │ register
┌───────────────────▼─────────────────────────┐
│  Tier 1: Instrumentation                    │
│  MBeans (관리 대상 객체들)                   │
│  - Memory, GC, Thread, OS, Runtime...       │
│  - 사용자 정의 MBean                        │
└─────────────────────────────────────────────┘
```

- **MBean**: 노출할 상태/명령 정의
- **MBeanServer**: 모든 MBean을 등록해서 관리 (ObjectName으로 조회)
- **Connector**: 원격 클라이언트가 MBeanServer 접근 (RMI 기본)

#### MBean 종류

| 종류 | 특징 | 쓰임 |
|-----|------|------|
| **Standard MBean** | 인터페이스 (`XxxMBean` 명명 규약) + impl 클래스 | 가장 흔함 |
| **Dynamic MBean** | `DynamicMBean` 인터페이스 구현. 메타정보 런타임 결정 | 동적 attribute가 필요할 때 |
| **Open MBean** | 표준 타입(primitive, String, Date 등)만 사용 | 다른 언어와 상호운용 |
| **Model MBean** | 설정 기반 generic MBean | XML/annotation로 선언 |
| **MXBean** ⭐ | Open MBean의 간편 버전. JDK platform이 사용 | **플랫폼 모니터링 (Memory, GC 등)** |

#### Platform MXBeans — JVM이 기본 제공 ⭐

JVM이 자동으로 등록하는 핵심 MBean들 (`java.lang.management.ManagementFactory`로 접근):

**verified (JDK 21, G1 기본)**:
```
Total MBeans: 27

Memory & Memory Pools (java.lang:type=MemoryPool / Memory):
  - Memory                       → heap / non-heap 전체
  - G1 Eden Space                → Young Eden
  - G1 Survivor Space            → Young Survivor
  - G1 Old Gen                   → Old
  - Metaspace                    → 클래스 메타데이터
  - Compressed Class Space       → 압축 클래스 포인터
  - CodeHeap 'non-nmethods'      → JVM 내부 코드
  - CodeHeap 'profiled nmethods' → JIT profiled 코드
  - CodeHeap 'non-profiled nmethods'

GC (java.lang:type=GarbageCollector):
  - G1 Young Generation          → Young GC 통계
  - G1 Concurrent GC             → Concurrent cycle 통계
  - G1 Old Generation            → Mixed/Full GC 통계

Memory Managers (java.lang:type=MemoryManager):
  - CodeCacheManager
  - Metaspace Manager

System:
  - Runtime                      → JVM 시작 시간, PID, 인자 등
  - Threading                    → thread 수, 상태, deadlock 감지
  - ClassLoading                 → 로드된 클래스 수
  - OperatingSystem              → CPU, 메모리 (OS 전체)
  - Compilation                  → JIT 통계
```

**Java 코드로 접근**:
```java
import java.lang.management.*;

MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
MemoryUsage heap = mem.getHeapMemoryUsage();
// init=300MB, used=5MB, committed=299MB, max=4718MB (verified JDK 21)

for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
    gc.getName();              // "G1 Young Generation"
    gc.getCollectionCount();   // GC 횟수
    gc.getCollectionTime();    // 누적 GC 시간 (ms)
}

ThreadMXBean th = ManagementFactory.getThreadMXBean();
th.getThreadCount();           // 현재 live thread 수
th.findDeadlockedThreads();    // deadlock 감지
```

#### 사용자 정의 MBean 예시

**Standard MBean 패턴** — 인터페이스 + impl:
```java
// 1. 인터페이스 (반드시 "XxxMBean" 접미사)
public interface OrderServiceMBean {
    long getProcessedOrders();        // read attribute
    int getActiveThreads();           // read attribute
    void resetCounters();             // operation
}

// 2. 구현
public class OrderService implements OrderServiceMBean {
    private AtomicLong processed = new AtomicLong();
    public long getProcessedOrders() { return processed.get(); }
    public int getActiveThreads() { return Thread.activeCount(); }
    public void resetCounters() { processed.set(0); }
}

// 3. 등록
MBeanServer server = ManagementFactory.getPlatformMBeanServer();
server.registerMBean(new OrderService(), new ObjectName("com.example:type=OrderService"));
```

→ JConsole로 접속하면 `com.example:type=OrderService` 노드에서 attribute 조회/명령 실행 가능.

#### 원격 접속 활성화

```bash
java \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9010 \
  -Dcom.sun.management.jmxremote.local.only=false \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -jar app.jar
```

⚠️ **인증/SSL 끄면 보안 취약**. 운영 환경에선 반드시 활성화.

#### 접근 도구

| 도구 | 특징 |
|-----|------|
| **JConsole** | JDK 내장, 가장 간단 |
| **VisualVM** | plugin으로 JMX 지원 |
| **JDK Mission Control (JMC)** | 가장 강력 (JFR 통합) |
| **Jolokia** | JMX의 **HTTP 브리지** — HTTP로 MBean 조회 |
| `jconsole` CLI, `jmxterm` | 커맨드 라인 |

---

## 2부. 현재 JMX는 쓰이는가 — 2026 관점

#### JVM 내부: **여전히 핵심** ✅

- Platform MXBeans는 **JVM 모든 운영/모니터링의 기반**
- JConsole, JMC, VisualVM 모두 내부적으로 JMX 사용
- `jcmd`, `jstat`도 일부 기능이 JMX 통해 동작

#### 애플리케이션 레벨: **사용 감소** ⚠️

**2008 ~ 2015 초반**:
- JMX가 사실상 유일한 Java 관리 API
- Nagios/Zabbix 등으로 JMX attribute 수집
- 애플리케이션에 커스텀 MBean 많이 작성

**2015 ~ 2026 현재**:
- **애플리케이션 메트릭은 HTTP 기반이 대세**
- Prometheus pull 모델 + Grafana 조합이 사실상 표준
- JMX 원격 접속의 **본질적 문제들**:
  - **RMI 기반** → firewall/NAT 투과 어려움
  - **Java 직렬화 보안 이슈** (여러 CVE)
  - **cloud-native (K8s) 환경에 어색** — pod IP 변경, service discovery 복잡
  - **HTTP처럼 캐시/프록시 불가**
  - 메트릭 **포맷 표준화 X** (JMX attribute 이름 임의)

→ 결과: **"JVM 내부 메트릭은 JMX, 애플리케이션 메트릭은 HTTP 기반 도구"**로 분화.

#### 현재 대체제/주류 도구 ⭐

**① Micrometer** — Metrics facade
- SLF4J의 metrics 버전. 여러 백엔드로 export 가능
- **Spring Boot 2+ 기본** (`spring-boot-starter-actuator`)
- Prometheus, Graphite, Datadog, New Relic 등 export 지원
```java
MeterRegistry registry = ...;
registry.counter("orders.processed").increment();
registry.timer("api.latency").record(Duration.ofMillis(100));
```

**② Spring Boot Actuator** — HTTP 엔드포인트
- `/actuator/health` — health check
- `/actuator/metrics` — 메트릭 (JSON)
- `/actuator/prometheus` — Prometheus 포맷
- `/actuator/env`, `/actuator/loggers`, `/actuator/threaddump`, `/actuator/heapdump`
- **JMX 엔드포인트도 노출 가능** (`/actuator/jolokia`)
- 사실상 "JMX의 HTTP 버전"

**③ Prometheus + Grafana** — 수집/저장/시각화
- 애플리케이션이 `/actuator/prometheus`로 메트릭 노출
- Prometheus가 주기적으로 **pull** 수집
- Grafana로 dashboard
- **Pull 모델** 장점: service discovery 쉬움, 애플리케이션은 수집 로직 몰라도 됨

**④ OpenTelemetry (OTel)** — 최신 통합 표준 ⭐
- **Metrics + Traces + Logs 통합** standard
- Vendor-neutral (Datadog, New Relic, Honeycomb, Jaeger, Prometheus 전부 지원)
- Java agent로 **코드 변경 없이** 추적 가능
- CNCF 프로젝트, 2021+ 급성장
- 추세: **Micrometer → OpenTelemetry로 전환 중**

**⑤ APM** (상용) — Datadog / New Relic / Dynatrace / Elastic APM
- Agent 설치 → JMX 메트릭 + bytecode instrumentation + 트레이스 자동 수집
- 내부적으로 JMX 접근도 함
- Dashboard, alert, 분산 추적 통합

**⑥ JDK Mission Control + JFR** — JVM 레벨 심층 분석
- JMX 기반 UI이지만 **JFR 데이터가 핵심**
- 저오버헤드로 운영 환경 상시 활성화 가능
- JMX보다 **더 깊은 정보** (allocation profile, lock contention 등)

**⑦ Jolokia** — JMX ↔ HTTP 브리지
- JMX의 가치를 유지하면서 **HTTP로 조회**
- Spring Boot Actuator에 통합 (`/actuator/jolokia`)
- **cloud 환경에서 JMX를 쓰고 싶을 때** 중간 다리

#### 비교표

| | **JMX** (2008 ~) | **Micrometer + Prometheus** (현재 주류) | **OpenTelemetry** (미래) |
|---|-----------------|---------------------------------------|------------------------|
| 프로토콜 | **RMI** (binary) | HTTP (text) | HTTP/gRPC |
| 데이터 모델 | Attribute/Operation | Counter/Gauge/Histogram/Summary | Metric/Trace/Log 통합 |
| Cloud/K8s 친화 | ❌ | ✅ | ✅ |
| 코드 변경 | **MBean 작성 필요** | API 호출 몇 줄 | Auto-instrumentation 가능 |
| Vendor 중립 | O (표준) | △ (facade이지만 Prometheus 선호) | **O (명확히 중립)** |
| 보안 | 복잡 (RMI + 인증) | 간단 (HTTP + 기존 보안 레이어) | 간단 |
| JVM 내부 메트릭 | ✅ (기본) | △ (JVM MXBeans → Micrometer bridge 필요) | △ (JMX bridge 사용) |

#### 현재 추천 조합 (2026)

**일반 Spring Boot 서비스**:
```
Spring Boot Actuator
  ├─ /actuator/health          (health check — K8s probe)
  ├─ /actuator/prometheus      (Micrometer → Prometheus 포맷)
  ├─ /actuator/metrics         (JSON 쿼리용)
  └─ /actuator/jolokia         (JMX 필요 시)

→ Prometheus (pull 수집)
→ Grafana (시각화 + alert)
→ (옵션) OpenTelemetry Collector (traces/logs 통합)
```

**JVM 심층 분석이 필요할 때**:
- JFR + JMC
- 상시 저오버헤드 수집, 사후 분석

**레거시 JMX 모니터링 유지하면서 HTTP 전환**:
- Jolokia로 bridge

---

## 3부. 책의 Story 20 (2008) → 현재 (2026)

**과거 모델** (책 집필 시):
- 커스텀 MBean 작성이 흔함
- JConsole로 접속해서 attribute 확인
- Nagios/Zabbix JMX plugin으로 모니터링

**현재 모델**:
- **JVM 레벨** 메트릭 (Memory, GC, Thread): JMX 유지 (Platform MXBeans)
- **애플리케이션 레벨** 메트릭: Micrometer + Prometheus
- **분산 추적**: OpenTelemetry
- **심층 분석**: JFR + JMC

→ JMX는 **"죽지 않았지만 영역이 좁아졌다"**. JVM 내부용으로 남고, 애플리케이션 노출은 HTTP 기반 도구들이 담당.

#### 아직 JMX 커스텀 MBean 쓰면 좋은 경우

- **운영 중 설정 변경** 필요 (log level, feature flag) — Spring Boot는 Actuator의 `/loggers` 사용
- **운영 중 명령 실행** (cache clear, flush 등) — JMX의 operation은 여전히 편함
- **레거시 시스템과 통합**
- **매우 세밀한 JVM tuning** (Platform MXBean만 쓰면 됨)

**새 애플리케이션 설계**: 커스텀 MBean 대신 **Actuator endpoint**로 노출하는 게 더 자연스러움.

---

## 참고 자료

- [Java Management Extensions (JMX) Technology](https://docs.oracle.com/en/java/javase/21/management/)
- [`java.lang.management` javadoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.management/java/lang/management/package-summary.html)
- [Micrometer](https://micrometer.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [OpenTelemetry for Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Jolokia](https://jolokia.org/) — JMX ↔ HTTP bridge
- [Prometheus](https://prometheus.io/)
