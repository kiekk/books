### Story 11. JSP와 서블릿, Spring에서 발생할 수 있는 여러 문제점

## 1부. Spring `@Transactional`의 동작 방식 (AOP 기반)

#### 큰 그림

```java
@Service
public class OrderService {
    @Transactional
    public void place(Order o) {
        repo.save(o);
        paymentService.charge(o);
    }
}
```

호출자가 `orderService.place(o)`를 호출하면 **실제로는 Spring이 만든 Proxy 객체**가 호출됨:

```
클라이언트 ──▶ OrderService$Proxy
                   │
                   ├─ 1) TransactionInterceptor가 가로챔
                   ├─ 2) PlatformTransactionManager.getTransaction()  (BEGIN)
                   ├─ 3) 실제 OrderService.place() 호출
                   │     └─ repo.save(o)
                   │     └─ paymentService.charge(o)
                   ├─ 4) 정상 종료 → commit()
                   │   예외 발생 → rollback() (조건 충족 시)
                   └─ 5) 리턴
```

#### Spring 소스로 verify — `TransactionAspectSupport.invokeWithinTransaction()`

Spring 6.1.10 verified:
```java
protected Object invokeWithinTransaction(Method method, Class<?> targetClass,
        final InvocationCallback invocation) throws Throwable {
    ...
    TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);

    Object retVal;
    try {
        // This is an around advice: Invoke the next interceptor in the chain.
        // This will normally result in a target object being invoked.
        retVal = invocation.proceedWithInvocation();   // ← 실제 메서드 실행
    }
    catch (Throwable ex) {
        // target invocation exception
        completeTransactionAfterThrowing(txInfo, ex);  // ← rollback 결정
        throw ex;
    }
    finally {
        cleanupTransactionInfo(txInfo);
    }
    ...
    commitTransactionAfterReturning(txInfo);           // ← commit
    return retVal;
}
```

핵심 단계:
1. `TransactionAttributeSource`로부터 `@Transactional` 메타데이터 추출 (propagation, isolation, rollbackFor 등)
2. `createTransactionIfNecessary()` — `PlatformTransactionManager.getTransaction()` 호출 → DB connection 획득 + autoCommit=false + `BEGIN`
3. `invocation.proceedWithInvocation()` — 실제 비즈니스 메서드 실행
4. 정상 종료 → `commit()`
5. Throwable catch → `completeTransactionAfterThrowing()` — `txAttr.rollbackOn(ex)` 체크 후 rollback or commit

#### `rollbackOn(ex)` 기본 규칙 — ⚠️ 흔한 함정

| 예외 종류 | 기본 동작 |
|----------|---------|
| `RuntimeException` (unchecked) | **rollback** ✅ |
| `Error` | **rollback** ✅ |
| **checked `Exception`** (`IOException`, 사용자 정의 Exception...) | **commit** ⚠️ |

이게 Spring의 유명한 함정: `throws IOException` 같은 체크드 예외는 **기본적으로 rollback 안 됨**. 명시적으로:
```java
@Transactional(rollbackFor = Exception.class)   // 모든 예외에 rollback
public void foo() throws IOException { ... }
```

#### 트랜잭션 전파 규칙 (`Propagation`) — 짧게

| Propagation | 트랜잭션 없을 때 | 트랜잭션 있을 때 |
|-------------|---------------|-----------------|
| `REQUIRED` (기본) | 새로 생성 | 참여 |
| `REQUIRES_NEW` | 새로 생성 | **일시 중단 + 새로 생성** |
| `SUPPORTS` | 트랜잭션 없이 실행 | 참여 |
| `MANDATORY` | 예외 | 참여 |
| `NEVER` | 트랜잭션 없이 실행 | 예외 |
| `NOT_SUPPORTED` | 트랜잭션 없이 실행 | 일시 중단 |
| `NESTED` | 새로 생성 | savepoint 생성 (중첩) |

---

## 2부. Spring Proxy 2종 — JDK Dynamic Proxy vs CGLIB

#### Spring의 선택 로직 (verified `DefaultAopProxyFactory`)

```java
public AopProxy createAopProxy(AdvisedSupport config) {
    if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
        Class<?> targetClass = config.getTargetClass();
        if (targetClass.isInterface() || Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)) {
            return new JdkDynamicAopProxy(config);         // ← 인터페이스
        }
        return new ObjenesisCglibAopProxy(config);         // ← 클래스
    }
    else {
        return new JdkDynamicAopProxy(config);             // ← 기본
    }
}
```

**결정 플로우**:
1. `proxyTargetClass=true` 설정 or 인터페이스 없음 → **CGLIB**
2. 타겟이 인터페이스/람다 → **JDK Dynamic Proxy**
3. 기본 (인터페이스 있고 설정 변경 없음) → **JDK Dynamic Proxy**

**Spring Boot는 기본 CGLIB** (`spring.aop.proxy-target-class=true`가 default).

#### ① JDK Dynamic Proxy

**원리**: `java.lang.reflect.Proxy.newProxyInstance()`를 사용해 **런타임에 인터페이스 구현체 생성**.

```java
MyService proxy = (MyService) Proxy.newProxyInstance(
    MyService.class.getClassLoader(),
    new Class[] { MyService.class },
    new InvocationHandler() {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Before advice
            System.out.println("호출 전: " + method.getName());
            Object result = method.invoke(target, args);   // 실제 호출
            // After advice
            System.out.println("호출 후");
            return result;
        }
    }
);
```

**특징**:
- **인터페이스가 반드시 필요** (interface implements)
- 생성 비용 적음
- JDK 내장 (`java.lang.reflect`)
- 메서드 호출마다 `InvocationHandler.invoke` → `Method.invoke` (리플렉션) 체인 → 약간 느림

**바이트코드 생성 결과** (개념적):
```java
class $Proxy0 implements MyService {
    private final InvocationHandler h;

    public void doWork(Order o) {
        h.invoke(this, Method<doWork>, new Object[]{o});
    }
    // ... 인터페이스의 모든 메서드가 h.invoke로 위임됨
}
```

#### ② CGLIB (Code Generation Library)

**원리**: 바이트코드를 생성해 **대상 클래스를 상속하는 서브클래스** 만듦. `MethodInterceptor`가 호출 가로챔.

```java
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(MyService.class);
enhancer.setCallback(new MethodInterceptor() {
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy mp) throws Throwable {
        // Before advice
        Object result = mp.invokeSuper(obj, args);   // super.method() 호출
        // After advice
        return result;
    }
});
MyService proxy = (MyService) enhancer.create();
```

**특징**:
- **인터페이스 불필요** (상속만 하면 됨)
- 바이트코드 생성 비용 있음 (처음 1회)
- `MethodProxy.invokeSuper()`는 리플렉션 없이 **FastClass 메커니즘**으로 호출 → JDK Proxy보다 빠른 편
- Spring Framework 안에 내장 (`org.springframework.cglib.proxy.Enhancer`)
- Spring 4+부터 `Objenesis` 함께 사용 → 생성자 우회해서 인스턴스 생성 (constructor 호출 안 하고 proxy 생성 가능)

**바이트코드 생성 결과** (개념적):
```java
class MyService$$EnhancerBySpringCGLIB$$abc123 extends MyService {
    private MethodInterceptor interceptor;

    @Override
    public void doWork(Order o) {
        interceptor.intercept(this, Method<doWork>, new Object[]{o}, MethodProxy);
    }
    // ... 상속 가능한 모든 메서드 override
}
```

#### 두 Proxy 비교

| 항목 | JDK Dynamic Proxy | CGLIB |
|------|-------------------|-------|
| 메커니즘 | 인터페이스 구현 (리플렉션) | 클래스 상속 (바이트코드 생성) |
| **인터페이스 필요** | ✅ 필수 | ❌ 불필요 |
| **final class** 지원 | 상관없음 (interface면 OK) | ❌ 상속 불가 → 적용 안 됨 |
| **final method** 지원 | 상관없음 | ❌ override 불가 → advice 무시됨 |
| **private method** 지원 | ❌ 인터페이스에 없음 | ❌ override 불가 |
| **static method** 지원 | ❌ | ❌ |
| 생성자 호출 | 안 함 | Spring+Objenesis 조합 시 안 함 |
| 생성 비용 | 낮음 | 중간 (바이트코드 생성) |
| 호출 비용 | 리플렉션 (`Method.invoke`) | FastClass (리플렉션 X) |
| Spring 기본 | 과거 (5.x 이전) | **현재 (Boot 기본)** |

---

## 3부. Proxy의 특징과 한계 ⭐

Proxy 기반 AOP가 **못 하는 것들**과 이유:

#### 한계 1 — `private method` ❌

```java
@Service
public class OrderService {
    public void place(Order o) {
        validate(o);   // ← 같은 클래스의 private 메서드 호출
    }

    @Transactional   // ⚠️ 적용 안 됨
    private void validate(Order o) { ... }
}
```

**이유**:
- Proxy는 **외부에서 호출될 때만** 가로챌 수 있음
- Private 메서드는 **외부에서 호출 불가** → proxy 체인에 안 들어감
- CGLIB: `private`은 상속 불가 → override 못 함
- JDK Proxy: 인터페이스에 private 선언 안 됨

#### 한계 2 — `final method` ❌ (CGLIB에서)

```java
@Service
public class OrderService {
    @Transactional   // ⚠️ CGLIB에선 적용 안 됨
    public final void place(Order o) { ... }
}
```

**이유**:
- CGLIB은 상속으로 override → `final`은 override 불가
- 조용히 실패 — 트랜잭션 안 걸리지만 **에러 메시지도 없음** (JDK 21 Spring 6에선 WARN 로그는 뜸)
- JDK Dynamic Proxy는 interface 기반이라 `final method`도 OK (interface 자체엔 final 없음)

#### 한계 3 — `final class` ❌ (CGLIB에서)

```java
public final class OrderService { ... }   // ⚠️ CGLIB 상속 불가
```

- 아예 proxy 생성 자체가 실패 → bean 등록 실패
- Kotlin은 **클래스가 기본 final** → `all-open` 플러그인 필요

#### 한계 4 — `static method` ❌

```java
public static void utility() {
    // @Transactional 무효 (인스턴스 메서드 아님)
}
```

- Static은 인스턴스에 바인딩되지 않음 → proxy 대상 아님

#### 한계 5 — **self-invocation** ⭐⭐⭐ (가장 흔한 함정)

```java
@Service
public class OrderService {
    public void outer(Order o) {
        inner(o);          // ⚠️ this.inner()와 동일 — proxy 안 거침!
    }

    @Transactional
    public void inner(Order o) { ... }
}
```

**현상**: `outer()` 호출 → proxy가 받음 → **실제 OrderService.outer() 실행** → `inner()` 호출은 **`this.inner()`** → **원본 객체의 inner()** 직접 호출 → **proxy 안 거침** → `@Transactional` 무시.

**원인**:
- Spring이 주입하는 bean은 **proxy 객체**
- 하지만 `outer()` 안의 `this`는 **실제 OrderService 인스턴스** (proxy가 아님)
- `this.inner()`는 당연히 proxy 안 거침

**workaround**:
```java
// 1) Self-injection (추천)
@Service
public class OrderService {
    @Autowired
    private OrderService self;   // 자기 자신 주입 (실제론 proxy)

    public void outer(Order o) {
        self.inner(o);           // ✅ proxy를 통해 호출
    }

    @Transactional
    public void inner(Order o) { ... }
}

// 2) ApplicationContext에서 bean 조회
// 3) 메서드를 다른 클래스로 분리

// 4) AspectJ 사용 (4부 참고) — 이게 가장 깔끔
```

#### 한계 6 — Constructor는 가로채지 못함

Proxy는 **메서드 호출만** 가로챔. 생성자는 CGLIB가 상속할 때 자동으로 호출되지만 (Objenesis 조합이면 안 됨), **advice가 안 적용**됨.

---

## 4부. AspectJ — Proxy 한계를 넘어서

Proxy는 "**런타임에 객체를 감싸는**" 접근. AspectJ는 "**바이트코드 자체를 수정**"하는 접근 → **private/final/static/self-invocation 전부 가능**.

#### AspectJ의 3가지 Weaving (바이트코드 삽입 시점)

**① Compile-Time Weaving (CTW)**
```
.java → ajc (AspectJ 컴파일러) → .class (aspect 삽입됨)
```
- 빌드 시 `ajc`로 컴파일
- 가장 빠름 (런타임 오버헤드 없음)
- 모든 메서드 가능 (private/final/static)

**② Post-Compile Weaving (Binary)**
```
.class → aspectjtools → .class (aspect 삽입됨)
```
- 이미 컴파일된 JAR을 사후 가공
- 외부 라이브러리에도 aspect 적용 가능

**③ Load-Time Weaving (LTW)**
```
.class → ClassLoader (로딩 중 바이트코드 수정) → JVM
```
- Java agent (`-javaagent:aspectjweaver.jar`)가 class loader에 끼어듦
- 클래스 로딩 시 aspect 삽입
- 유연함 (설정으로 ON/OFF)
- 약간의 로딩 비용

#### Spring + AspectJ 조합

Spring은 두 가지 지원:
- **Spring AOP** (기본) — Proxy 기반. 위에서 설명한 한계 있음.
- **AspectJ 모드** — `@EnableLoadTimeWeaving` or `@EnableAspectJAutoProxy(proxyTargetClass = true)` 등

**`@Transactional`에서 AspectJ 사용** (self-invocation 문제 해결):
```java
@Configuration
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)   // ← proxy 대신 AspectJ
public class TxConfig { ... }
```

JVM 실행 시:
```bash
java -javaagent:spring-instrument.jar -jar app.jar
```
+ Spring의 AspectJ aspect JAR (`spring-aspects`) 포함.

이렇게 하면:
- private 메서드에도 `@Transactional` 적용 ✅
- self-invocation도 트랜잭션 동작 ✅
- final 메서드도 가능 ✅

#### Proxy AOP vs AspectJ 비교

| 항목 | Spring Proxy AOP | AspectJ |
|------|------------------|---------|
| 적용 시점 | 런타임 (proxy 생성) | 컴파일/로드 타임 (바이트코드 수정) |
| private 메서드 | ❌ | ✅ |
| final 메서드/클래스 | ❌ (CGLIB) | ✅ |
| self-invocation | ❌ | ✅ |
| static 메서드 | ❌ | ✅ |
| 생성자 | ❌ | ✅ |
| field access | ❌ (메서드만) | ✅ (field read/write도) |
| 설정 복잡도 | 낮음 | 높음 (javaagent, 빌드 플러그인) |
| 성능 오버헤드 | 메서드당 proxy 호출 | 더 낮음 (바이트코드 inline) |
| Spring 기본 | ✅ | 선택 |

#### 어떻게 가능한가 — 바이트코드 삽입 예시

원본:
```java
@Transactional
private void inner(Order o) {
    repo.save(o);
}
```

AspectJ CTW 후 (개념적):
```java
private void inner(Order o) {
    TransactionAspect.aspectOf().enter(/* joinpoint */);
    try {
        repo.save(o);                          // 원본 코드
        TransactionAspect.aspectOf().commit();
    } catch (Throwable ex) {
        TransactionAspect.aspectOf().rollback(ex);
        throw ex;
    }
}
```

→ **메서드 바이트코드 자체에 트랜잭션 로직이 박힘**. private이든 final이든 상관없음. self-invocation도 **같은 메서드에 삽입된 코드가 실행**됨.

---

## 5부. Spring 트랜잭션 실무 함정 모음

- **Checked Exception**: 기본 rollback 안 됨 → `rollbackFor` 지정
- **Self-invocation**: 같은 클래스 내 호출은 proxy 안 거침
- **`@Transactional`을 private에 붙임**: proxy면 무효
- **readOnly=true**: 성능 힌트일 뿐, **JPA에선 Hibernate가 dirty check 생략**해서 체감됨. JDBC Template만 쓰면 효과 미미
- **트랜잭션 안에서 외부 API 호출**: 커넥션 보유한 상태로 HTTP 대기 → 커넥션 풀 고갈
- **LazyInitializationException**: 트랜잭션 끝난 뒤 proxy entity 접근 → 세션 없음 → 예외
- **`@Async` + `@Transactional`**: 둘 다 proxy 기반인데 순서 꼬임. 보통 `@Async`가 바깥, `@Transactional`이 안쪽에 있어야 함
- **생성자에 트랜잭션 못 걸음**: Spring이 bean 생성 완료 전엔 proxy 없음. constructor에서 DB 접근 금지

---

## 참고 자료

- Spring 6.1.10 소스: `TransactionAspectSupport`, `DefaultAopProxyFactory`
- Spring Framework reference — "Aspect-Oriented Programming with Spring"
- "Pro Spring AOP" (AspectJ 심화)
- CGLIB documentation
