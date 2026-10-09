### Story 07. 클래스 정보, 어떻게 알아낼 수 있나?

## 1부. Reflection 기본

#### Reflection이 가능한 이유 — Class metadata

JVM은 로드한 모든 클래스의 메타데이터(필드, 메서드, 생성자, 어노테이션, 수퍼클래스 등)를 **Metaspace**에 유지함. `Class<?>` 객체가 그 창구.

```java
Class<?> clazz = String.class;                          // 리터럴
Class<?> clazz2 = Class.forName("java.lang.String");    // 문자열로 로드
Class<?> clazz3 = someObject.getClass();                // 인스턴스로부터
```

#### 핵심 API

```java
// 필드
Field[] fields = clazz.getDeclaredFields();
Field f = clazz.getDeclaredField("name");
f.setAccessible(true);              // private 접근 허용
f.set(instance, "value");           // 값 설정
Object v = f.get(instance);         // 값 조회

// 메서드
Method[] methods = clazz.getDeclaredMethods();
Method m = clazz.getDeclaredMethod("doSomething", String.class);
Object result = m.invoke(instance, "arg");

// 생성자
Constructor<?> ctor = clazz.getDeclaredConstructor(String.class);
Object obj = ctor.newInstance("arg");

// 어노테이션
MyAnno anno = clazz.getAnnotation(MyAnno.class);
boolean has = clazz.isAnnotationPresent(Controller.class);
```

#### 왜 "느리다"고 하나

- **메서드/필드 lookup 비용**: `getDeclaredMethod()`는 메서드 테이블을 순회하며 이름/시그니처 매칭. 호출당 수 μs.
- **`Method.invoke()` 비용**: 매개변수 박싱(Object[] 생성), 접근 권한 체크, 가변인자 처리 등 오버헤드. JIT이 인라인 못 해서 최적화 제한.
- **접근 권한 체크**: `setAccessible(true)` 호출 비용. JDK 17+에선 모듈 캡슐화 때문에 더 비싸짐.

→ 호출 1회 비용은 "ns 단위 메서드 호출"의 100~1000배. 다만 **캐싱하면 상당 부분 상쇄 가능** (아래 Spring 사례 참고).

---

## 2부. Spring 생태계에서 Reflection을 쓰는 곳

> Spring Framework 6.1.10 소스로 verified.

#### ✅ 1) Spring MVC — DispatcherServlet의 Handler 탐지 (verified)

사용자님 가설이 **정확**합니다. `DispatcherServlet` 뒤의 `RequestMappingHandlerMapping`이 애플리케이션 시작 시 **모든 bean을 reflection으로 스캔**해서 handler 메타데이터를 구축함.

**실제 Spring 6.1.10 소스 체인**:

```java
// 1. RequestMappingHandlerMapping.java
protected boolean isHandler(Class<?> beanType) {
    return AnnotatedElementUtils.hasAnnotation(beanType, Controller.class);
    //     ↑ 각 bean 클래스에 @Controller 어노테이션 있는지 reflection으로 확인
}

// 2. AbstractHandlerMethodMapping.java — @Controller면 메서드 iterate
protected void detectHandlerMethods(Object handler) {
    Class<?> handlerType = ...;
    Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
        (MethodIntrospector.MetadataLookup<T>) method ->
            getMappingForMethod(method, userType)   // @RequestMapping 어노테이션 추출
    );
    methods.forEach((method, mapping) -> {
        registerHandlerMethod(handler, invocableMethod, mapping);
    });
}

// 3. MethodIntrospector.java — ReflectionUtils로 위임
public static <T> Map<Method, T> selectMethods(Class<?> targetType, MetadataLookup<T> metadataLookup) {
    ...
    ReflectionUtils.doWithMethods(currentHandlerType, method -> { ... });
    ...
}

// 4. ReflectionUtils.java — 결국 Java reflection 호출
public static void doWithMethods(Class<?> clazz, MethodCallback mc, @Nullable MethodFilter mf) {
    Method[] methods = getDeclaredMethods(clazz, false);
    //                 ↑ 결국 Class.getDeclaredMethods() 호출
    ...
}
```

**플로우 요약**:
1. 애플리케이션 시작 → `DispatcherServlet` 초기화
2. `BeanFactory`가 모든 bean을 iterate
3. 각 bean의 **클래스에 `@Controller` 어노테이션 있는지 reflection check**
4. 있으면 **그 클래스의 모든 메서드 reflection으로 iterate**
5. 각 메서드에서 `@RequestMapping`, `@GetMapping` 등 **메타데이터 reflection으로 추출**
6. `RequestMappingInfo` 생성 → `URL → HandlerMethod` 맵에 등록

→ **런타임 요청 처리는 미리 만들어둔 맵을 조회**할 뿐이라 reflection 안 씀. 비용은 **시작 시 1회에만** 집중.

#### ✅ 2) Spring Core — Bean 생성과 DI

**Bean 인스턴스화**:
```java
// AbstractAutowireCapableBeanFactory 내부
Constructor<?> ctor = beanClass.getDeclaredConstructor(paramTypes);
Object bean = ctor.newInstance(args);
```
→ 설정에 선언된 bean을 `Class.forName()`으로 로드하고, `getDeclaredConstructor().newInstance()`로 생성.

**필드 주입 (`@Autowired` on field)**:
```java
// AutowiredAnnotationBeanPostProcessor 내부
Field f = targetClass.getDeclaredField(fieldName);
f.setAccessible(true);
f.set(bean, resolvedDependency);
```
→ `@Autowired` 붙은 필드를 reflection으로 찾고 값 주입. private 필드도 `setAccessible(true)`로 접근.

**설정 값 주입 (`@Value`)**:
- 필드/매개변수의 `@Value("${prop.name}")` 어노테이션 추출 → reflection
- 환경변수에서 값 조회 → 필드에 reflection으로 설정

#### ✅ 3) Spring Boot — Auto Configuration (verified)

Spring Boot의 핵심 "자동 설정"은 reflection으로 돌아감.

**verified 소스**:
```java
// SpringFactoriesLoader.java
return factoryImplementationClass.getDeclaredConstructor().newInstance();
```

**플로우**:
1. 기동 시 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일 읽음 (여러 jar에서 수집)
2. 파일 안엔 `com.example.AutoConfigurationA`, `com.example.AutoConfigurationB` 같은 **클래스 이름 문자열** 목록
3. 각 클래스를 `Class.forName()`으로 로드 (**reflection**)
4. `@Conditional` 평가:
   - `@ConditionalOnClass("Foo")`: `Class.forName("Foo")`로 존재 여부 확인
   - `@ConditionalOnMissingBean(Bar.class)`: BeanFactory에 이미 Bar bean이 있는지 확인
   - 등등 — 전부 reflection 기반 메타데이터 분석
5. 통과한 설정만 `getDeclaredConstructor().newInstance()`로 등록

→ "클래스패스에 라이브러리를 넣기만 하면 자동으로 설정되는 마법"의 정체는 **reflection으로 조건부 bean 등록**.

#### ✅ 4) Spring Security — Method Security

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) { ... }
```

**동작**:
1. `MethodSecurityInterceptor` (AOP 기반)가 메서드 호출 가로챔
2. 메서드의 어노테이션 reflection으로 추출: `method.getAnnotation(PreAuthorize.class)`
3. SpEL(`hasRole('ADMIN')`) 평가 — 평가 과정에서 메서드 **파라미터 이름까지 reflection**으로 추출 (`-parameters` 컴파일 옵션 필요)
4. 통과하면 메서드 실행, 아니면 `AccessDeniedException`

→ Spring Security 3.1+의 메서드 레벨 security는 전부 reflection + AOP 조합.

#### ✅ 5) Spring Data JPA — Entity와 Repository

**Entity 매핑**:
```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue
    private Long id;

    @Column(name = "user_name")
    private String name;
}
```

- Hibernate(Spring Data JPA의 provider)가 entity 클래스를 **reflection으로 분석**
- `@Entity`, `@Table`, `@Id`, `@Column` 어노테이션 메타데이터 추출
- ResultSet → Entity 변환:
  - 리플렉션으로 private 필드 직접 set (`Field.set()`) — 기본 모드
  - 또는 property mode에서 setter 호출 (`Method.invoke()`)

**Repository**:
```java
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByUsernameAndStatus(String username, int status);
}
```

- `JpaRepositoryFactoryBean`이 Repository **interface만 보고** 구현체를 proxy로 생성 (JDK Dynamic Proxy = reflection)
- 메서드 호출 시 메서드명 파싱: `findByUsernameAndStatus` → `username = ? AND status = ?`
- Query derivation 과정에서 Entity 클래스의 field 이름을 reflection으로 매칭

→ **코드 한 줄 안 짜고 쿼리가 나오는 마법 = reflection + proxy의 합작**.

#### ✅ 6) Spring AOP — Proxy 생성

**JDK Dynamic Proxy** (interface 기반):
```java
Proxy.newProxyInstance(classLoader, interfaces, invocationHandler);
```
- `Proxy.newProxyInstance` 자체가 reflection API
- `InvocationHandler.invoke(proxy, method, args)` 안에서 `method.invoke(target, args)` 호출

**CGLIB** (클래스 기반):
- 바이트코드를 **생성**하는 방식이라 pure reflection은 아니지만, 메서드 매칭/시그니처 분석은 reflection
- `@Transactional`, `@Async`, `@Cacheable` 등은 모두 CGLIB proxy 생성 (interface 없이도 가능)

**Pointcut 매칭**:
- `@Pointcut("execution(* com.example.service.*.*(..))")`
- AspectJ가 메서드 서명(이름, 파라미터 타입, 리턴 타입)을 reflection으로 분석해서 매칭 판정

#### ✅ 7) 기타

- **Jackson** (Spring Boot 기본 JSON): bean property 분석 (getter/setter reflection), `@JsonProperty` 어노테이션
- **Validation** (`@Valid`, `@NotNull`): 필드 어노테이션 reflection으로 추출
- **Scheduling** (`@Scheduled`): 메서드 어노테이션 reflection으로 cron 설정 추출
- **Caching** (`@Cacheable`): 메서드 메타데이터 reflection
- **`BeanUtils.copyProperties`**: 두 bean 간 reflection으로 필드 복사

---

## 3부. Reflection 비용과 Spring의 최적화

#### Reflection은 느리다 — 얼마나?

| 작업 | 상대 비용 (대략) |
|------|---------------|
| 직접 메서드 호출 | 1x (baseline) |
| `Method.invoke()` (캐싱된 Method) | 3~10x |
| `Method.invoke()` (매번 lookup) | 100~1000x |
| `Field.get/set()` (캐싱) | 2~5x |
| `getDeclaredMethods()` lookup | 100~10000x (클래스 크기에 비례) |

→ **lookup이 가장 비쌈**. invoke 자체는 캐싱하면 상당히 상쇄됨.

#### Spring의 대응 — 캐싱 + AOT

1. **`ReflectionUtils`가 Method 캐싱**:
   ```java
   // Spring Core 내부
   private static final Map<Class<?>, Method[]> declaredMethodsCache = new ConcurrentReferenceHashMap<>(256);
   ```
   한 번 `getDeclaredMethods()`한 결과를 class별로 캐싱 → 같은 클래스 재조회는 O(1).

2. **애플리케이션 시작 시 1회 reflection, 런타임엔 미리 만든 맵 조회**:
   - `RequestMappingHandlerMapping`도 시작 시 모든 매핑을 미리 reflection으로 구축 → 요청 처리는 reflection 없이 맵 조회만.

3. **Spring Framework 6 / Spring Boot 3 — AOT (Ahead-of-Time) + GraalVM Native Image**:
   - **빌드 타임에 reflection 사용처를 분석**해서 metadata 파일로 미리 추출
   - Native Image는 reflection을 **런타임에 거의 안 쓰고** 빌드 결과에 하드코딩
   - 시작 시간 수 초 → 수십 ms, 메모리 사용량 대폭 감소

#### JDK 수준의 대안

| 기법 | 등장 | 특징 |
|-----|------|------|
| `MethodHandle` (`java.lang.invoke`) | JDK 7 | reflection보다 빠름. JIT 친화적 |
| `VarHandle` | JDK 9 | 필드 접근 최적화. `AtomicXxx` 대체 가능 |
| `Lookup.unreflect(Method)` | JDK 7 | Method → MethodHandle 변환 |
| AOT (jlink/jaotc, GraalVM) | JDK 9+/16+ | 빌드 시점 분석 |

Spring Framework는 핵심 경로에서 점점 `MethodHandle` 쪽으로 이동 중.

---

## 4부. 실무 함정

- **`setAccessible(true)` + JDK 17+**: 모듈 캡슐화 때문에 외부 모듈의 non-public 멤버 접근 시 경고/에러. `--add-opens` JVM 플래그 필요한 경우 있음.
- **익명 클래스/람다의 메서드 이름**: 익명 클래스는 `$1`, 람다는 `lambda$method$0` 같은 이름. reflection으로 다루면 깨지기 쉬움.
- **제네릭 타입 소거**: `List<String>`에서 reflection으로 `String`을 못 가져옴 (type erasure). `ParameterizedType`을 통해 간접 추출.
- **`-parameters` 컴파일 옵션**: Spring MVC가 메서드 파라미터 **이름**을 쓰려면 이 옵션 필수 (`@RequestParam("id")` 안 쓸 때 특히).
  - Maven: `<parameters>true</parameters>`
  - Gradle: `tasks.withType(JavaCompile) { options.compilerArgs << '-parameters' }`

---

## 부록. Metaspace에는 또 뭐가 저장되나

> ⚠️ Story 6에서 "static 필드가 Metaspace에 저장된다"고 썼는데 **그건 정확하지 않음**. 아래가 바로잡은 정리.

#### Metaspace에 저장되는 것 (JDK 8+)

1. **Klass 메타데이터** (per-class, HotSpot의 C++ 자료구조)
   - 클래스 이름 (`"com.example.User"`)
   - 수퍼클래스 포인터
   - 구현 인터페이스 리스트
   - access flags (`public`, `final`, `abstract` 등)
   - **method table (vtable)** — 가상 메서드 디스패치용
   - **field layout** — 각 인스턴스 필드의 offset 정보 (어느 field가 몇 바이트 뒤에 있는지)

2. **Method 메타데이터** (per-method)
   - **바이트코드 자체** — JIT 컴파일 전엔 interpreter가 이걸 직접 읽어 실행
   - 메서드 시그니처, 디스크립터
   - exception table (try-catch 범위)
   - local variable table (디버깅/스택 트레이스용)
   - line number table

3. **Runtime Constant Pool**
   - `.class` 파일의 constant pool을 로드 시 복원한 것
   - 메서드/필드 심볼릭 참조 (resolve 전)
   - 숫자 상수, 클래스 참조 등

4. **어노테이션 메타데이터** — 리플렉션으로 꺼내는 그 어노테이션 정보

5. **Class loader 메타데이터**
   - `ClassLoaderData` 구조체
   - 각 로더가 로드한 클래스 목록

6. **Compressed Class Space** (JDK 8+ 세부 영역, Metaspace의 하위)
   - `-XX:+UseCompressedClassPointers` (기본 ON)
   - 64비트 포인터 압축을 위한 보조 공간
   - 기본 1GB (JDK 21 verified: `CompressedClassSpaceSize = 1073741824`)

#### Metaspace에 저장되지 **않는** 것 (혼동 주의)

| 저장 위치 | 내용 |
|---------|------|
| **Heap** | 모든 인스턴스 객체, **`java.lang.Class` 미러 객체**, **⭐ static 필드 값** |
| **Heap (String Pool 영역)** | 리터럴 문자열 `"foo"`, `"bar"` (JDK 7+ 이동) |
| **Code Cache** | JIT 컴파일된 네이티브 코드 |
| **Thread Stacks** | 지역변수, 매개변수, operand stack |
| **Direct Memory** | `ByteBuffer.allocateDirect()` 등 off-heap |

#### ⭐ static 필드는 실제로 어디 있나 — JDK 세대별

```
JDK 6:   Klass 구조 안 (PermGen)
JDK 7:   java.lang.Class 미러 객체 안 (heap)   ← bug 6964458 fix로 이동
JDK 8+:  java.lang.Class 미러 객체 안 (heap)   ← Metaspace로 안 넘어갔음. 그대로 heap
```

JDK 8에서 PermGen → Metaspace 전환할 때:
- **class 메타데이터**(Klass)만 Metaspace로 이동
- **static 필드 값**은 **이미 JDK 7에 heap으로 옮겨져 있었기 때문에** 그대로 heap 유지

흔한 오해 — "JDK 8에서 PermGen에 있던 걸 전부 Metaspace로 옮겼겠지"가 아니라, "**이미 heap에 있던 건 그대로, Klass 메타데이터만 Metaspace로**".

**단, field의 "정의"는 Metaspace에 있음**:
- `User.class`의 field 목록(이름, 타입, static인지 instance인지, static field라면 mirror 내 offset) → **Metaspace (Klass 안)**
- static field의 **실제 값** → **heap (mirror 안)**
- 비유: Metaspace는 "설계도", heap은 "실제 창고 자리"

#### 왜 분리됐나 (설계 의도)

- **Klass 메타데이터**: 거의 안 변함, 클래스 수명 동안 유지. native memory에 두는 게 효율적 (GC 영향 X).
- **static 필드 값 & mirror**: Java 코드가 "그냥 객체"처럼 접근해야 함 (`User.class`로 reflection, `Class.getName()` 호출 등). heap에 둬야 GC와 자연스럽게 통합.
- **String Pool**: 간헐적으로 리터럴 수거되기 때문에 Minor GC 참여가 유리 → heap.

#### 실무 모니터링

```bash
java -XX:MaxMetaspaceSize=256m -XX:MetaspaceSize=128m -jar app.jar
```

**Metaspace OOM 전형 원인**:
- **ClassLoader leak**: 톰캣 재배포 반복, OSGi 번들 반복 load/unload
- **동적 클래스 생성 과다**: CGLIB/ASM/ByteBuddy로 매 요청마다 proxy 클래스 생성 (anti-pattern)
- **대량 lambda** — 매 lambda 호출마다 synthetic class가 생기는 건 아니지만, invokedynamic bootstrap에서 간접적으로 메타데이터 증가

**모니터링 커맨드**:
```bash
jcmd <pid> VM.classloader_stats    # ClassLoader별 Metaspace 사용량
jcmd <pid> GC.class_histogram      # 클래스별 인스턴스 수
```

---

## 참고 자료

- Spring Framework 소스: `spring-core`, `spring-webmvc` 6.1.10 (gradle cache + maven central sources)
- `org.springframework.util.ReflectionUtils` javadoc
- `org.springframework.core.MethodIntrospector` javadoc
- JEP 122: Remove the Permanent Generation (JDK 8)
- JEP 274: Enhanced Method Handles (JDK 9)
- JEP 193: Variable Handles (JDK 9)
- Spring Boot Reference — AOT and Native Image support
- HotSpot source: `src/hotspot/share/oops/instanceKlass.hpp`, `src/hotspot/share/classfile/javaClasses.cpp`
