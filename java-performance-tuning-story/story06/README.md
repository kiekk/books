### Story 06. static 제대로 한번 써 보자.

## 1부. `final static` — 왜 쓰는가 (메모리 관점)

#### 메모리 레이아웃 — static field는 어디 있나

JVM의 런타임 메모리 구조 (JDK 8+):

```
┌─────────────────────────────────────────────────────┐
│ Heap                                                │
│  ├─ Young Generation (Eden + Survivor 0/1)         │
│  ├─ Old Generation                                 │
│  └─ String Pool (JDK 7+)                           │ ← 리터럴 "foo", "bar"
│                                                     │
├─────────────────────────────────────────────────────┤
│ Metaspace (Native memory, JDK 8+)                   │
│  ├─ Class metadata (Class<?> 정보, method table)   │
│  └─ static field 저장소 ★                           │
│                                                     │
├─────────────────────────────────────────────────────┤
│ Thread Stacks                                       │
│  └─ 지역변수, 매개변수, operand stack              │
└─────────────────────────────────────────────────────┘
```

**핵심 포인트:**
- `static` 필드 자체(참조 변수)는 **Metaspace의 클래스 구조 안에 저장**됨.
- 그 참조가 가리키는 **실제 객체는 heap**에 존재.
- 예: `static final List<String> CODES = List.of("A", "B");`
  - `CODES`라는 참조 슬롯 → Metaspace
  - `ImmutableList` 객체 → heap

#### JDK 7/8 전후 변화 (메모리 영역 재편)

| 영역 | ~JDK 6 | JDK 7 | JDK 8+ |
|-----|--------|-------|--------|
| Class 메타데이터 | PermGen | PermGen | **Metaspace** (native) |
| static 필드 | PermGen | **heap** | **Metaspace** |
| String Pool | PermGen | **heap** | **heap** |

- **JDK 7**: String Pool이 PermGen → heap으로 이동. 아래 "String Pool 이전의 성능 향상 메커니즘" 상세.
- **JDK 8**: PermGen 완전 제거, Metaspace로 대체 (JEP 122). Metaspace는 native memory에 할당되어 기본 상한선 없음 (`MaxMetaspaceSize` 미설정 시 OS 메모리 한계까지).
  - 과거의 `java.lang.OutOfMemoryError: PermGen space` 가 `java.lang.OutOfMemoryError: Metaspace` 로 바뀜.

#### JDK 7 String Pool 이전 — 어떻게 성능이 향상됐나

**JDK 6까지의 문제점** (String Pool이 PermGen에 있을 때):

1. **PermGen 공간 제약**: PermGen 기본 크기 64MB~128MB. `intern()`을 많이 호출하면 `OutOfMemoryError: PermGen space` 발생. "큰 로그 데이터 처리" 같은 상황에서 터지기 쉬움.
2. **GC 참여 제한**: PermGen은 **Full GC 때만** 수거됨 (Minor GC는 Young만 처리). 임시로 intern된 문자열이 **Full GC 발생 전까진 상주** → heap 압박.
3. **고정된 작은 hash table**: `StringTable`의 기본 크기 **1009** (bucket 수). 수백만 개 intern하면 bucket 당 체인이 길어져서 lookup `O(1)` 평균이 깨짐. hash collision 심화.
4. **class metadata와 공간 다툼**: PermGen 안에서 class 로딩 정보와 공유. intern 많이 하면 클래스 로딩 공간이 깎임.

**JDK 7 변경 사항**:

1. **String Pool을 heap으로 이동** (bug 6962931 fix, JDK 7). 이제 Young/Old 세대의 일반 GC cycle에 참여.
2. **기본 StringTableSize 증가**: 1009 → 60013 (JDK 7u40). 수만 개 intern해도 lookup O(1) 유지.
3. **StringTableSize 튜닝 플래그 제공**: `-XX:StringTableSize=N` 으로 bucket 수 명시적 조정.

**JDK 8**: PermGen 완전 제거. String Pool은 그대로 heap 유지. Class metadata만 Metaspace로 이동.

**성능 영향 비교**:

| 항목 | JDK 6 (PermGen) | JDK 7+ (heap) |
|------|---------------|--------------|
| intern pool 최대 크기 | PermGen 크기 제약 (64~128MB) | heap 크기까지 (수 GB) |
| 임시 intern된 문자열 수거 | **Full GC 때만** | **Minor GC 때도 가능** |
| hash table 기본 크기 | 1009 | 60013 (7u40+) → 65536 (JDK 21 verified) |
| lookup 시간 (수만 개 intern 후) | 체인 길어져 느림 | 거의 O(1) 유지 |
| 전형적 OOM | `PermGen space` 자주 발생 | heap OOM만 |

**실측 (JDK 21)**:
```java
for (int i = 0; i < 100_000; i++) ("prefix_" + i).intern();
```
```
default (StringTableSize=65536):  23 ms
-XX:StringTableSize=131072:        21 ms
```

→ **왜 빨라졌나 요약**:
- **"공간 자유도"**: PermGen 좁은 공간 → heap의 넓은 공간. OOM 걱정 축소.
- **"GC 참여"**: Full GC 때만 → 매 Minor GC마다 임시 intern 문자열 수거 가능. heap 압박 감소.
- **"hash table 크기"**: 1009 → 60013+. collision 체인 짧게 유지 → 조회/삽입 O(1) 유지.

세 가지가 복합적으로 작용해서 `intern()` 자체 속도 + 전체 애플리케이션 안정성 둘 다 향상됨.

#### `final static` 이 주는 보장

```java
public class Constants {
    public static final int MAX_RETRY = 3;
    public static final String API_URL = "https://api.example.com";
    public static final Map<String, Integer> CODES = Map.of("A", 1, "B", 2);
}
```

1. **클래스 로딩 시 1회 초기화**: `<clinit>` 메서드가 class loading 중 **단 한 번** 실행.
2. **애플리케이션 전체에서 공유**: 인스턴스마다 복제되지 않음. 1000개 객체가 있어도 상수 메모리는 1개.
3. **재할당 불가**: `final` 때문에 레퍼런스 교체 X. (단, 참조하는 객체 **내부 상태**는 변할 수 있음. `List.of()`나 `Collections.unmodifiableXxx()`를 함께 써야 immutable 보장.)
4. **스레드 안전**: 초기화는 JVM이 happens-before 보장 (JLS §12.4). 이후 **읽기만 하면** 동기화 불필요.

#### JIT 최적화 혜택 — constant folding

JIT 컴파일러는 `static final` 중 **원시 타입 + 컴파일 시점 상수**를 **코드에 직접 inline**함:

```java
public static final int MAX = 3;

if (count < MAX) { ... }
// ↓ JIT 최적화 후 (constant folding)
if (count < 3) { ... }   // 상수가 바이트코드에 박힘
```

- 변수 로드/비교 → 상수 비교로 축소
- JIT이 branch prediction, loop unrolling 등 추가 최적화 적용 가능

**단, 객체 참조는 constant folding 안 됨**. `static final Map<...>` 같은 참조는 매번 field load.

#### "자주 쓰고 절대 안 변하는" 데이터의 전형

```java
// ✅ 상수
public static final int BUFFER_SIZE = 8192;

// ✅ 설정 (불변)
public static final String API_URL =
    System.getProperty("api.url", "https://api.example.com");

// ✅ 코드성 데이터 (lookup table)
public static final Map<String, String> COUNTRY_CODES = Map.of(
    "KR", "대한민국",
    "US", "미국",
    "JP", "일본"
);

// ✅ 재사용 가능한 compiled 리소스
public static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
public static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
```

**왜 매번 생성하지 않고 static final로?**
- `Pattern.compile()`은 수천 명령어 소비. 매번 호출하면 CPU + GC 압박.
- 1회 생성 + 공유 → 메모리/CPU 모두 절약.

---

## 2부. 메모리 릭 — `static`의 그림자

#### static 참조는 **애플리케이션 수명과 운명을 같이함**

클래스가 unload되기 전까지 (사실상 거의 never) static 필드가 참조하는 객체는 **GC가 절대 수거하지 않음**. 이는 **의도적**(상수) vs **의도치 않음**(leak)의 양날의 검.

#### 전형적 leak 패턴 ① — 끝없이 자라는 static 캐시

```java
// 책 안티패턴
public class UserService {
    private static final Map<Long, User> cache = new HashMap<>();   // ⚠️

    public User find(Long id) {
        return cache.computeIfAbsent(id, k -> loadFromDB(k));
    }
}
```

- 사용자가 늘어날수록 `cache` 무한 증가.
- static이라 **영원히 heap 점유**.
- 결국 OOM.

**해결**: eviction 정책 있는 캐시 사용 (`ConcurrentHashMap` + 수동 size 제한, Caffeine, Guava Cache).

#### 전형적 leak 패턴 ② — `ThreadLocal` without `remove()`

```java
private static final ThreadLocal<HeavyObject> local = new ThreadLocal<>();

void handle(Request r) {
    local.set(new HeavyObject(r));
    // ...
    // local.remove()를 호출 안 함
}
```

- `ThreadLocalMap`은 스레드마다 보관. 톰캣/스프링 환경에서 스레드가 재사용되면 **이전 요청의 HeavyObject가 영원히 살아있음**.
- 스레드 풀 크기 × HeavyObject 크기 = heap 상주 메모리.

**해결**: `try { local.set(...); ... } finally { local.remove(); }` 패턴 강제.

#### 전형적 leak 패턴 ③ — 리스너/옵저버 등록만 하고 해제 안 함

```java
public class EventBus {
    private static final List<Listener> listeners = new ArrayList<>();
    public static void register(Listener l) { listeners.add(l); }
}

// 어딘가에서
eventBus.register(new MyComponent());   // ⚠️ 해제 코드 없음
```

- `MyComponent` 인스턴스가 리스너 리스트에 걸려서 **영원히 참조됨**.
- 사용자가 "이 컴포넌트는 더 이상 안 쓴다"라고 생각해도 heap에 남음.

**해결**: `WeakReference` 기반 리스너 리스트 or 명시적 `unregister()` 강제.

#### 실측 데모 (방금 확인)

```java
static final Map<String, byte[]> cache = new HashMap<>();
for (int i = 0; i < 10000; i++) cache.put("key" + i, new byte[1024]);  // 10MB
System.gc();
// used memory: 12,590 KB  ← System.gc()에도 안 사라짐
```

- static 참조가 **GC Root** 역할 → 아무리 `System.gc()` 호출해도 `cache` 안 내용은 reachable.

---

## 3부. GC와의 연결점 — `static`이 **GC Root**인 이유

#### Reachability — 생존/제거 판정 알고리즘

Garbage Collector는 "**reachable한 객체**"를 **살려두고** 나머지를 **수거**함.

Reachable의 정의: **GC Root로부터 참조 체인을 따라 도달 가능한 모든 객체**.

```
GC Root → objA → objB → objC
          ↓
          objD
```
→ A, B, C, D 모두 살려둠.

```
objX → objY     (아무도 X를 참조하지 않음)
```
→ X, Y 수거.

#### GC Roots에는 뭐가 있는가

HotSpot JVM에서 GC Root로 취급되는 것들:

1. **활성 스레드의 스택 프레임에 있는 지역 변수** — 현재 실행 중 메서드의 로컬 참조
2. **활성 스레드 자체** — Thread 객체
3. **클래스의 static 필드** ★ — 로딩된 모든 클래스의 static 필드
4. **JNI 참조** — native 코드가 쥐고 있는 참조
5. **synchronized 블록에서 사용 중인 모니터 객체**
6. **JVM 내부 참조** — Class 객체, 시스템 클래스 로더 등

→ **`static` 필드로 참조되는 모든 객체는 GC가 절대 수거하지 않음** (해당 클래스가 unload되기 전까지). 이게 static을 상수에 쓰면 좋은 이유 (영구 보존)이자, 캐시에 쓰면 위험한 이유 (영구 leak).

#### 클래스 unload는 거의 일어나지 않음

클래스가 unload되려면:
- 그 클래스를 로드한 **ClassLoader가 GC**되어야 함
- ClassLoader가 GC되려면 그를 참조하는 모든 참조가 사라져야 함
- 보통 애플리케이션 수명 = ClassLoader 수명 = 클래스 수명 = static 필드 수명

→ 사실상 **애플리케이션 종료 전까지 static 필드는 살아있다**.

---

## 4부. GC 동작 / 알고리즘 기본

#### Mark-and-Sweep 핵심

가장 기본 GC 알고리즘:

1. **Mark 단계**: GC Root부터 시작해 reachable한 모든 객체에 "live" 표시 (bit set)
2. **Sweep 단계**: 표시 안 된 객체를 수거 (해당 메모리를 free list에 반환)

문제: **fragmentation**. free 공간이 여기저기 흩어져서 큰 객체 할당 어려움.

→ **Mark-Compact**: sweep 대신 live 객체를 한쪽으로 **밀어 모아서** 연속 공간 확보.
→ **Copying**: heap을 반반으로 나누고, 한쪽에서 다른 쪽으로 live 객체만 복사.

#### Weak Generational Hypothesis (약한 세대 가설)

> "대부분의 객체는 **금방 죽는다**. 오래 산 객체는 **더 오래 산다**."

실측상 압도적으로 참 (대부분 애플리케이션에서 90%+ 객체가 한두 번의 메서드 콜 안에 가비지가 됨).

이를 활용해 heap을 세대로 분할:

```
┌──────────────────────────────────────────────────┐
│ Young Generation                                  │
│  ┌──────┐  ┌──────────┐  ┌──────────┐           │
│  │ Eden │  │ Survivor │  │ Survivor │           │
│  │      │  │    0     │  │    1     │           │
│  └──────┘  └──────────┘  └──────────┘           │
├──────────────────────────────────────────────────┤
│ Old Generation                                    │
└──────────────────────────────────────────────────┘
```

- **Eden**: 새 객체 할당 지역. 거의 모든 객체가 여기서 태어나고 여기서 죽음.
- **Survivor 0 / 1**: Eden에서 살아남은 소수 객체가 번갈아 이동.
- **Old (Tenured)**: Survivor를 N번 (기본 15회) 왕복하며 생존한 "old-age" 객체.

#### Minor GC vs Major GC vs Full GC

| 종류 | 대상 | 빈도 | 비용 (STW 시간) |
|-----|------|------|---------------|
| **Minor GC** | Young만 | 자주 (초당 수십 회) | 짧음 (수 ms) |
| **Major GC** | Old만 (일부 collector) | 드물게 (수 분~시간) | 길음 (수십 ms~초) |
| **Full GC** | Young + Old + Metaspace | 매우 드물게 | 가장 긴 STW |

- Minor GC는 보통 **Copying 알고리즘** (Eden → Survivor 복사). 빠름.
- Full GC는 전체 heap을 훑어야 하므로 느림. **Full GC 횟수는 모니터링해야 할 핵심 지표**.

#### Stop-The-World (STW) vs Concurrent

- **STW**: GC 작업 중 **모든 애플리케이션 스레드 일시 정지**. 전통적 GC가 이렇게 동작. 서비스 레이턴시에 치명적.
- **Concurrent**: GC 작업 중 일부를 **애플리케이션 스레드와 병행 실행**. STW 시간 최소화. 대신 CPU 오버헤드 증가.
- 현대 GC(G1, ZGC, Shenandoah)는 **대부분 concurrent + 짧은 STW phase 몇 개**.

---

## 5부. JDK 버전별 GC 알고리즘

#### 타임라인

| JDK | 기본 GC | 주요 변화 |
|-----|---------|---------|
| 1.0 ~ 1.3 | Serial | 유일. 작은 heap용 single-thread GC |
| 1.4 | Serial | **Parallel GC**, **CMS** 도입 (옵션) |
| 5 | **Parallel** (server-class) | 서버 머신에서 ergonomic 선택 |
| 6 ~ 7 | Parallel | **G1 도입** (JDK 7, 실험적) |
| 8 | Parallel | G1 production ready (8u40). **PermGen 제거, Metaspace** |
| **9** | **G1** ★ | 기본 G1로 변경. CMS **deprecated** |
| 11 | G1 | **ZGC 실험적**, **Epsilon**(no-op), **Shenandoah 실험적** |
| 12 | G1 | Shenandoah 메인라인 합류 |
| 14 | G1 | **CMS 완전 제거** |
| 15 | G1 | **ZGC production**, **Shenandoah production** |
| 16 | G1 | ZGC concurrent stack scanning |
| 17 LTS | G1 | 안정화 (특별한 큰 변화 없음) |
| **21 LTS** | G1 | **Generational ZGC** (JEP 439) |
| 24 | G1 | **Generational Shenandoah** (JEP 404 완성) |
| 25 LTS | G1 | 지속적 ZGC/G1 개선 |

#### 각 GC 특성 요약

| GC | 특징 | 적합한 상황 |
|-----|------|----------|
| **Serial** | Single-thread, STW. 가장 단순. | heap < 100MB, single-core, CLI 도구 |
| **Parallel** | Multi-thread Young+Old GC. STW. **throughput 최대화**. | 배치, 처리량 중요한 서버 |
| **CMS** (제거됨) | Concurrent Old GC. Young은 ParNew. 레거시. | — (JDK 14+에서 삭제) |
| **G1** (Garbage First) | Region 기반 (heap을 4~32MB region으로 쪼갬). 짧은 STW 목표. | 범용 (JDK 9+ default). heap 4GB~100GB |
| **ZGC** | **Sub-millisecond STW**. heap 수백 GB~TB까지. colored pointers. | 대규모 heap, 저지연 요구 |
| **Shenandoah** | ZGC와 유사한 저지연 목표. Red Hat 개발. forwarding pointer 사용. | ZGC 대안, 저지연 |
| **Epsilon** | **No-op GC**. 수거 안 함, OOM 터지면 종료. | 벤치마크, 짧은 수명 앱 |

#### 각 GC 동작 방식 상세

##### Serial GC

**Young generation (Minor GC) — Copying**
1. Eden이 꽉 참 → STW 시작 (모든 app 스레드 정지)
2. Eden + 사용 중 Survivor의 **live 객체만** → 반대편 Survivor로 복사
3. 복사 횟수(`age`)가 임계값(기본 15) 초과 → **Old 승격(promotion)**
4. 복사가 끝난 Eden + 이전 Survivor는 **통째로 비움** (dead 객체 분리 안 함)
5. STW 종료

**Old generation (Major GC) — Mark-Sweep-Compact**
1. STW 시작
2. **Mark**: GC Root부터 DFS로 reachable 객체 전부 표시
3. **Sweep**: 표시 안 된 객체의 메모리 영역을 free list에 반환
4. **Compact**: live 객체를 heap 앞쪽으로 밀어 모아 fragmentation 제거
5. STW 종료

→ **모든 작업이 단일 스레드 + STW**. heap 커지면 느려짐.

##### Parallel GC

Serial과 **동일 알고리즘**, 차이는 **GC 작업을 여러 스레드가 분담**.

- **Young**: Multi-thread Copying. Eden을 N개 조각으로 나눠 N개 GC 스레드가 각자 복사.
- **Old**: Multi-thread Mark-Sweep-Compact (JDK 7u4+). 각 phase 안에서 작업 분할.
- 여전히 **STW**이지만, 코어 수만큼 병렬화 → STW 시간 1/N.
- `-XX:ParallelGCThreads=N`으로 스레드 수 조절.

→ CPU 코어를 전부 써서 throughput 최대. 지연은 여전히 긺.

##### CMS (Concurrent Mark Sweep) — 제거됨

Old GC를 **concurrent**로 돌리는 혁신. phase 구성:

1. **Initial Mark** (STW, 짧음): GC Root가 **직접 참조하는** 객체만 mark
2. **Concurrent Mark**: app 실행 중에 전체 참조 그래프 탐색. app도 동시에 참조를 수정 → write barrier로 추적
3. **Remark** (STW, 비교적 짧음): concurrent mark 중 변경된 참조를 재검증 (SATB — Snapshot At The Beginning)
4. **Concurrent Sweep**: unreachable 객체를 free list로 반환. app 실행 중

**한계**:
- **Compact 안 함** → fragmentation 누적 → 큰 객체 할당 실패하면 **Full GC fallback** ("concurrent mode failure")
- Full GC는 Serial 수준으로 느려서 서비스 멈춤
- JDK 9에서 deprecated, JDK 14에서 완전 제거

##### G1 (Garbage First)

Heap을 **Region**(기본 4MB, 1~32MB)으로 쪼갬. Region 레이블: Young(Eden/Survivor) / Old / Humongous(큰 객체 전용).

**Young GC** (STW, 짧음):
1. 모든 Young region의 live 객체를 **evacuate**(복사) → 다른 Survivor region or Old로 승격
2. 비워진 Young region은 재사용 가능 pool에 반환

**Concurrent Marking Cycle** (G1 중 가장 비싼 작업):
1. **Initial Mark** (Young GC에 piggyback, STW): GC Root 직접 참조 mark
2. **Root Region Scan** (concurrent): Survivor region의 참조 추적
3. **Concurrent Mark**: 전체 Old region의 live 데이터 측정 (app 실행 중)
4. **Remark** (STW): SATB write barrier 로 변경사항 재검증
5. **Cleanup**: 완전히 비어있는 region은 즉시 재사용 pool로 반환 (STW), 나머지 통계 수집 (concurrent)

**Mixed GC** (STW):
- "**Garbage First**" 전략: concurrent marking 결과로 **가장 많이 비어있는 Old region부터** 수거 후보 선정
- Young GC + 선별된 Old region 몇 개를 함께 evacuate
- 전체 heap을 한 번에 수거하지 않고 **점진적으로 Old 정리** → 긴 STW 회피

**특징**: STW 목표 pause 시간을 **`-XX:MaxGCPauseMillis=200`로 지정 가능**. G1이 알아서 수거량 조절.

##### ZGC (Z Garbage Collector)

핵심 기술 두 가지:
- **Colored pointers**: 64비트 참조 중 상위 몇 비트를 **GC 상태 metadata**로 사용 (marked, remapped 등). 참조 자체에 정보를 실어 나르는 아이디어.
- **Load barrier**: 코드가 참조를 **읽을 때마다** barrier 코드 실행 → bit 확인해서 outdated 참조면 즉시 fix-up.

**Phase 구성** (거의 모두 concurrent):
1. **Mark Start** (STW, 수백 μs): Root 집합 스냅샷
2. **Concurrent Mark**: 전체 그래프 traversal (app 실행 중). load barrier가 변경 추적
3. **Mark End** (STW, 짧음): 마무리 동기화
4. **Concurrent Relocate**: live 객체를 새 region으로 복사. app이 중간에 **오래된 주소를 읽어도 load barrier가 즉시 새 주소로 번역**해줌 → app이 멈출 필요 없음

**결과**: heap 크기와 무관하게 **STW < 10ms**. 수백 GB~TB heap 가능.

**JDK 21 Generational ZGC**:
- Young/Old 분리로 Minor GC처럼 Young만 자주 수거 → 처리량 ↑

##### Shenandoah

ZGC와 같은 목표(저지연, concurrent compaction), 다른 전략.

- **Brooks pointer** (forwarding pointer): **모든 객체에 추가 포인터 슬롯**을 심어둠. 평상시엔 자기 자신을 가리킴. 복사되면 새 위치를 가리키게 변경.
- **Read barrier**: 참조 읽을 때마다 forwarding pointer를 거쳐서 실제 객체 접근.
- Concurrent compaction: 복사된 객체를 app이 어느 포인터로 가리키든 forwarding pointer가 리다이렉트.

**Phase 구성**:
1. Initial Mark (STW)
2. Concurrent Mark
3. Final Mark (STW)
4. **Concurrent Evacuation**: live 객체 복사, forwarding pointer 갱신 (app 실행 중)
5. Init Update References (STW)
6. **Concurrent Update References**: 모든 참조를 새 주소로 업데이트
7. Final Update References (STW)

**결과**: ZGC와 비슷한 저지연. 구현 복잡도는 더 높고, 모든 객체에 포인터 하나씩 더 들어가는 오버헤드 있음.

##### Epsilon — "No-op GC"

- **Mark 안 함, Sweep 안 함**
- 그냥 **bump-pointer 할당만** (`alloc_ptr++`)
- heap 꽉 차면 `OutOfMemoryError` 던지고 끝
- 쓰임:
  - 벤치마크 (GC 노이즈 제거하고 순수 애플리케이션 성능 측정)
  - 극단적으로 짧은 수명 앱 (수초 안에 끝나서 GC 발동 전에 종료)
  - GC 관련 버그 디버깅

---

#### 동작 방식 요약 비교

| GC | Young 알고리즘 | Old 알고리즘 | STW 시간 | Compaction |
|----|--------------|-------------|---------|-----------|
| Serial | Copying (1-thread) | Mark-Sweep-Compact (1-thread) | 긺 | O |
| Parallel | Copying (N-thread) | Mark-Sweep-Compact (N-thread) | 중간 | O |
| CMS | ParNew Copying | **Concurrent** Mark-Sweep | 짧음 | **X** |
| G1 | Copying (region 단위) | Concurrent Mark + 점진 compact | 조절 가능 | O (점진) |
| ZGC | Copying + Colored Ptr | **Concurrent** 전부 | **< 10ms** | O (concurrent) |
| Shenandoah | Copying + Brooks Ptr | **Concurrent** 전부 | **< 10ms** | O (concurrent) |
| Epsilon | 없음 | 없음 | 0 | X |

#### Generational ZGC (JEP 439, JDK 21)

JDK 21 전까지 ZGC는 **non-generational** — 전체 heap을 한 번에 scan. 저지연은 좋지만 처리량이 G1보다 떨어졌음.

JDK 21 Generational ZGC: Young/Old 분리로 **Minor GC처럼 Young만 자주 수거** → ZGC의 저지연 + Parallel의 처리량 양쪽 노림.

```bash
java -XX:+UseZGC -XX:+ZGenerational -jar app.jar  # JDK 21
```

JDK 23부터는 `-XX:+ZGenerational`이 기본 (non-generational은 deprecated).

#### 현재 (JDK 21) 기준 선택 가이드

| 조건 | 추천 GC |
|-----|---------|
| 모를 때 | **G1** (default, 무난) |
| 처리량 최대, 짧은 응답지연 상관없음 | Parallel |
| 저지연 (p99 응답속도 중요) | **ZGC** (JDK 15+) |
| 매우 큰 heap (100GB+) | ZGC |
| 작은 heap (< 1GB), 단일 코어 | Serial |

실무 90%는 **G1 그대로**. 레이턴시 민감하면 ZGC 전환 고려.

---

## 6부. 실무 체크리스트

**`final static` 좋은 사용:**
- [ ] 원시 타입 상수 (`int`, `long`, `double`)
- [ ] 불변 리터럴 (`String` with no interpolation)
- [ ] 불변 컬렉션 (`List.of`, `Set.of`, `Map.of`, `Collections.unmodifiableXxx`)
- [ ] 재사용 가능한 compiled 리소스 (`Pattern`, `DateTimeFormatter`, `MessageDigest` 캐시)
- [ ] enum 상수

**경계해야 할 사용:**
- [ ] mutable collection (`static final List` 이지만 add/remove 호출됨) → leak 소지
- [ ] 캐시로 쓸 때 → eviction 정책 필수 (Caffeine/Guava 추천)
- [ ] `static ThreadLocal` → `remove()` 강제 필요
- [ ] `static` 리스너 리스트 → `WeakReference` or 명시적 해제

**GC 관련 모니터링 포인트:**
- [ ] Full GC 횟수 (분당 몇 회?)
- [ ] GC 소요 시간 (총 CPU 시간 대비 %)
- [ ] Young/Old 비율 변화 추이
- [ ] Metaspace 사용량 (JDK 8+에서 **class loading leak** 지표)

```bash
# JDK 21 GC 로그
java -Xlog:gc*=info -XX:+PrintGCDetails -jar app.jar
```

---

## 참고 자료

- JVM Spec §5.5 Initialization — static 초기화 순서
- JEP 122: Remove the Permanent Generation (JDK 8)
- JEP 248: G1 as default (JDK 9)
- JEP 291: Deprecate CMS (JDK 9)
- JEP 318: Epsilon (JDK 11)
- JEP 377: ZGC production (JDK 15)
- JEP 439: Generational ZGC (JDK 21)
- OpenJDK HotSpot GC Tuning Guide
