### Story 16. JVM은 도대체 어떻게 구동될까?

## 1부. HotSpot VM 아키텍처

#### 전체 그림

```
┌───────────────────────────────────────────────────┐
│              Java Application (.class)             │
└───────────────────────────────────────────────────┘
                        ▼
┌───────────────────────────────────────────────────┐
│      Class Loader Subsystem (로딩/링킹/초기화)      │
└───────────────────────────────────────────────────┘
                        ▼
┌───────────────────────────────────────────────────┐
│         Runtime Data Areas (메모리)                 │
│  ┌────────────┬────────────┬──────────────────┐  │
│  │   Heap     │ Metaspace  │  Thread Stacks   │  │
│  │ (객체)      │ (클래스/   │  (지역변수)      │  │
│  │             │  바이트코드)│                  │  │
│  └────────────┴────────────┴──────────────────┘  │
└───────────────────────────────────────────────────┘
                        ▼
┌───────────────────────────────────────────────────┐
│              Execution Engine                       │
│  ┌─────────────┬────────────┬────────────────┐    │
│  │ Interpreter │    JIT     │   GC (메모리    │    │
│  │ (바이트코드 │ Compiler   │   관리자)       │    │
│  │  해석 실행) │ (네이티브   │                 │    │
│  │             │  컴파일)    │                 │    │
│  └─────────────┴────────────┴────────────────┘    │
└───────────────────────────────────────────────────┘
                        ▼
┌───────────────────────────────────────────────────┐
│              JNI (Native Interface)                 │
└───────────────────────────────────────────────────┘
                        ▼
┌───────────────────────────────────────────────────┐
│                 Operating System                    │
└───────────────────────────────────────────────────┘
```

#### 3대 핵심 구성 요소

**① VM 런타임 (Execution Engine의 Interpreter + 지원 시스템)**

JVM이 바이트코드를 **직접 실행**하는 부분. HotSpot의 interpreter는 "template-based" 방식 — 각 바이트코드 명령에 대해 미리 generate된 native code template을 호출.

지원하는 하위 시스템:
- **Thread 관리** — Platform Thread (OS 1:1 매핑), Virtual Thread (JDK 21+)
- **Synchronization** — monitor, biased/lightweight/heavy lock
- **Exception handling** — exception table 조회, stack unwind
- **JNI** — native 코드 호출 (C/C++)
- **Signal handler** — SIGSEGV, SIGINT 처리

**② JIT 컴파일러**

Interpreter가 느리므로, 자주 실행되는 코드(**hot method**)를 **네이티브 코드로 컴파일**해서 재사용.

HotSpot의 JIT 컴파일러 2종:
- **C1 (Client compiler)** — 빠른 컴파일, 가벼운 최적화. 시작 시간 중요한 데스크톱용
- **C2 (Server compiler)** — 긴 컴파일 시간, 깊은 최적화. 서버용. **대부분 상황에서 더 빠른 코드**

JDK 7+ **Tiered Compilation** (기본 ON) — C1 → C2 승격 메커니즘:

| Tier | 설명 |
|------|------|
| 0 | Interpreter |
| 1 | C1 (최적화 없음, profiling 없음) |
| 2 | C1 (제한된 profiling) |
| 3 | **C1 (full profiling)** ★ |
| 4 | **C2 (profile 기반 최적화)** ★ |

실행 흐름: Interpreter → Tier 3 (C1 profiling) → Tier 4 (C2 fully optimized).

**verified JDK 21 실측** (`java -XX:+PrintCompilation JITTest`):
```
12  6     3    JITTest::sum (21 bytes)              ← Tier 3 (C1 profiling)
12  7 %   4    JITTest::sum @ 4 (21 bytes)          ← Tier 4 (C2) + OSR (%)
13  8     4    JITTest::sum (21 bytes)              ← Tier 4 (C2) 일반 호출
14  6     3    JITTest::sum (21 bytes) made not entrant   ← Tier 3 폐기
```

- `%` = **OSR (On-Stack Replacement)**: 긴 루프가 실행 중일 때 그 자리에서 compiled version으로 교체
- `made not entrant`: 승격 후 하위 버전 폐기 (더 이상 새 호출에 사용 안 됨)

verified default thresholds (JDK 21):
- `Tier3CompileThreshold = 2000` (호출 2000회 or 루프 반복 다수에서 C1 승격)
- `CompileThreshold = 10000` (최종 C2 승격 임계)

**③ 메모리 관리자 (Garbage Collector)**

[Story 06에서 상세 다룸](../story06/README.md#5부-jdk-버전별-gc-알고리즘). 간단 요약:
- **Serial / Parallel / G1 / ZGC / Shenandoah / Epsilon**
- JDK 21 기본 **G1**
- Heap 관리 외에도 Metaspace, Code Cache(JIT 결과물), Direct Memory 등 다양한 영역을 각자 관리

---

## 2부. Runtime Data Areas 상세 — JVM Spec §2.5

JVM 스펙은 **6가지 메모리 영역**을 정의. 1부의 아키텍처 다이어그램에선 간략화했지만, 스펙 기준으로 분해하면 아래와 같음.

#### 전체 구조

```
┌─────────────────── JVM Memory ──────────────────────┐
│                                                      │
│  ┌─────────── Thread별 (각 Thread 보유) ──────────┐  │
│  │                                                │  │
│  │  ┌────────┐  ┌──────────┐  ┌───────────────┐ │  │
│  │  │   PC   │  │   JVM    │  │  Native       │ │  │
│  │  │Register│  │  Stack   │  │  Method Stack │ │  │
│  │  └────────┘  └──────────┘  └───────────────┘ │  │
│  └────────────────────────────────────────────────┘  │
│                                                      │
│  ┌─────────── Thread 간 공유 ────────────────────┐  │
│  │                                                │  │
│  │  ┌───────────────────┐  ┌───────────────────┐│  │
│  │  │       Heap         │  │   Method Area      ││  │
│  │  │                    │  │  (= Metaspace, 8+) ││  │
│  │  │ ┌──────────────┐   │  │                    ││  │
│  │  │ │ String Pool  │   │  │ ┌────────────────┐ ││  │
│  │  │ │   (JDK 7+)   │   │  │ │Runtime Constant│ ││  │
│  │  │ └──────────────┘   │  │ │   Pool         │ ││  │
│  │  │ ┌──────────────┐   │  │ └────────────────┘ ││  │
│  │  │ │Class 미러 +   │   │  └────────────────────┘│  │
│  │  │ │static 필드 값│   │                        │  │
│  │  │ │  (JDK 7+)    │   │                        │  │
│  │  │ └──────────────┘   │                        │  │
│  │  └───────────────────┘                         │  │
│  └────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

#### ① PC Register (Program Counter Register)

| 속성 | 내용 |
|---|---|
| **범위** | Thread별 (각자 1개) |
| **역할** | 현재 실행 중인 바이트코드 명령의 주소 |
| **크기** | 매우 작음 (몇 바이트) |
| **OOM** | 없음 |
| **특이사항** | native 메서드 실행 중엔 "undefined" 상태 |

**JDK 25 현재**: 스펙 그대로 유지. 변화 없음.

#### ② JVM Stack

| 속성 | 내용 |
|---|---|
| **범위** | Thread별 |
| **역할** | 메서드 호출마다 **stack frame push**, 리턴 시 pop |
| **Stack Frame 구성** | Local variable array (지역변수, 매개변수) + Operand stack (연산용 임시) + Constant pool reference |
| **크기** | 기본 1 MB (`-Xss1m`) |
| **OOM** | `StackOverflowError` — 재귀 과다, 깊은 호출 |

**JDK 25 변화** — Virtual Thread (JDK 21+):
- Platform Thread: OS 스레드 stack = JVM stack
- **Virtual Thread**: stack이 **heap에 "continuation" 객체로 저장** (수 KB)
- Carrier Thread에 mount되면 OS stack 사용, unmount되면 heap으로 복귀
- 수백만 Thread 가능해진 핵심 이유

#### ③ Heap

| 속성 | 내용 |
|---|---|
| **범위** | 모든 Thread 공유 |
| **역할** | 인스턴스 객체, 배열 저장 |
| **크기** | `-Xms` (최초) ~ `-Xmx` (최대). JDK 21 기본 Max = 물리 RAM의 1/4 |
| **OOM** | `OutOfMemoryError: Java heap space` |
| **GC 대상** | ✅ (유일하게 GC가 완전 관리) |

**JDK 세대별 변화**:
- JDK 7+: **String Pool**이 heap으로 이동
- JDK 7+: **java.lang.Class 미러 + static 필드 값**이 heap에 저장
- JDK 7+: GC 알고리즘 다변화 (G1, ZGC, Shenandoah)

#### ④ Method Area

| 속성 | 내용 |
|---|---|
| **범위** | 모든 Thread 공유 |
| **역할** | 클래스 메타데이터, 메서드 바이트코드, 상수 풀, static 데이터 저장 |
| **구현 자유도** | 스펙은 "logical" 영역만 정의. HotSpot의 실제 이름은 다름 |

**JDK 세대별 구현 변화** ⭐:

| JDK | 구현 명 | 위치 |
|-----|-------|------|
| 1.0 ~ 7 | **PermGen** | Heap 안의 특수 영역 |
| **8 ~ 25** | **Metaspace** (JEP 122) | **Native memory** (heap 밖) |

**Metaspace에 저장되는 것** ([Story 07 부록](../story07/README.md#부록-metaspace에는-또-뭐가-저장되나) 참조):
- Klass 메타데이터 (vtable, field layout)
- Method 바이트코드
- Runtime Constant Pool
- 어노테이션 메타데이터
- ClassLoader 메타데이터

**OOM**: `OutOfMemoryError: Metaspace` — 주로 ClassLoader leak

**JDK 25 현재**: Metaspace 유지. 기본 상한 없음 (OS 메모리까지).

#### ⑤ Runtime Constant Pool

| 속성 | 내용 |
|---|---|
| **범위** | Method Area의 일부. **클래스/인터페이스마다 1개** |
| **역할** | `.class` 파일 constant pool을 로드 시 복원 |
| **포함** | 숫자 상수, 메서드/필드 심볼릭 참조, 클래스 참조 |
| **구현** | HotSpot: Metaspace의 각 Klass에 소속 |

**JDK 25 현재**: 동일.

**⚠️ String Pool과 혼동 금지**:

| 항목 | Runtime Constant Pool | String Pool |
|------|---------------------|------------|
| 범위 | 클래스별 (각각 보유) | 전역 1개 |
| 위치 | Metaspace | Heap (JDK 7+) |
| 저장 | 심볼릭 참조, 숫자 상수 | intern된 String 인스턴스 |

#### ⑥ Native Method Stack

| 속성 | 내용 |
|---|---|
| **범위** | Thread별 |
| **역할** | JNI를 통한 **native (C/C++) 메서드 호출** 시 사용 |
| **구현 자유도** | 아예 안 둬도 됨 (스펙상). HotSpot은 OS C stack 그대로 사용 |
| **OOM** | `StackOverflowError` (JVM stack과 공유 메시지) |

**JDK 25 현재**: 동일.

#### 전체 종합표

| 영역 | 범위 | 저장 내용 | OOM | JDK 25 변화 |
|------|------|---------|-----|-----------|
| PC Register | Thread | 현재 PC | — | 유지 |
| JVM Stack | Thread | Stack frame | `StackOverflowError` | **Virtual Thread는 heap 저장** |
| Heap | 공유 | 모든 객체 | `Java heap space` | GC 다양화 |
| **Method Area** | 공유 | 클래스 메타 | `Metaspace` | **PermGen → Metaspace (JDK 8)** |
| Runtime Constant Pool | 공유 (Method Area 내) | 심볼릭 참조 | `Metaspace` | 유지 |
| Native Method Stack | Thread | native stack | `StackOverflowError` | 유지 |

#### JVM 스펙 §2.5의 6개 영역 — JDK 25에서도 동일한가?

**정의는 그대로**. **구현만 바뀜**:

1. **Method Area**: PermGen → **Metaspace** (JDK 8, JEP 122)
2. **Heap**: String Pool 포함 (JDK 7+), Class 미러 + static 값 포함 (JDK 7+)
3. **JVM Stack**: Virtual Thread의 경우 heap에 저장 (JDK 21+)
4. **PC Register / Runtime Constant Pool / Native Method Stack**: 변화 없음

→ **"JVM 메모리 모델"은 매우 안정적**. 애플리케이션 코드가 신경 쓸 변화는 거의 없음. 운영/모니터링 관점에선 **"어느 영역 OOM인지"**가 가장 중요.

#### GC 알고리즘 리마인드 (Story 06 참조)

[Story 06 — 5부. JDK 버전별 GC 알고리즘](../story06/README.md#5부-jdk-버전별-gc-알고리즘)에서 상세 다뤘음. 짧게 복습:

| JDK | 기본 GC | 주요 변화 |
|-----|-------|---------|
| 1.0 ~ 1.3 | Serial | 유일 |
| 1.4 ~ 5 | Serial / Parallel | **Parallel, CMS 도입** |
| 7 | Parallel | **G1 도입** (실험적) |
| **8** | Parallel | G1 production, **PermGen 제거, Metaspace** |
| **9** | **G1 전환** | CMS deprecated |
| 11 | G1 | **ZGC/Shenandoah/Epsilon** 실험 |
| 14 | G1 | **CMS 완전 제거** |
| 15 | G1 | **ZGC/Shenandoah production** |
| **21 LTS** | G1 | **Generational ZGC** (JEP 439) |
| 24 | G1 | Generational Shenandoah |
| 25 LTS | G1 | 지속 개선 |

**현재 (JDK 25) 선택 가이드**:
- 범용 / 모를 때 → **G1** (default)
- 저지연 p99 민감 → **ZGC** (JDK 15+)
- 처리량 최대, 지연 상관없음 → **Parallel**
- 대규모 heap (100 GB+) → **ZGC**

**상세는 Story 06 5부**를 참조.

---

## 3부. JIT Optimizer — 어떤 최적화를 하나

#### 주요 최적화 종류

| 최적화 | 설명 | 예시 |
|-------|------|------|
| **Method Inlining** ⭐ | 작은 메서드 호출을 호출부에 직접 삽입 | `a.getX()` → `a.x` (field access) |
| **Escape Analysis** | 메서드 밖으로 안 나가는 객체 분석 | `new Point(1,2)` → stack 할당, scalar replacement |
| **Loop Unrolling** | 루프 반복을 펼침 | `for (i=0; i<4; i++)` → `s=a[0]+a[1]+a[2]+a[3]` |
| **Dead Code Elimination** | 안 쓰이는 코드 제거 | `int x = compute(); // x 사용 안 함` → 삭제 |
| **Constant Folding** | 상수 계산 미리 수행 | `3 * 4 + 1` → `13` |
| **Loop Invariant Code Motion** | 루프 내 불변 코드를 밖으로 | `for (i) { int c=a+b; use(c); }` → `int c=a+b; for (i) use(c);` |
| **Branch Prediction** | 자주 가는 분기를 fall-through로 재배치 | `if (likely) {...} else {...}` → fast path 재구성 |
| **Vectorization (SIMD)** | 반복을 SIMD 명령으로 | `sum += a[i]` 4개 묶어 1 명령 |
| **Deoptimization** ⭐ | 가정 틀리면 interpreter로 복귀 | class hierarchy 변경, type 예측 실패 등 |
| **OSR (On-Stack Replacement)** | 실행 중인 루프를 compiled 버전으로 교체 | `main`의 10억 번 돌 중 루프 |

#### Escape Analysis + Scalar Replacement 예시

```java
Point p = new Point(1, 2);
return p.x + p.y;
```

**Interpreter 수준 실행**:
1. heap에 Point 객체 할당
2. 필드에 1, 2 저장
3. 필드 두 개 읽음
4. 합산 리턴

**JIT 최적화 후** (escape analysis + scalar replacement):
```java
int x = 1;
int y = 2;
return x + y;
```
→ 객체 자체가 사라짐 (allocation 없음, GC 압박 없음).

#### Deoptimization — "가정이 틀렸을 때"

C2는 **profile 기반으로 가정**을 세우고 최적화. 예:
```java
Animal a = getAnimal();
a.sound();   // Dog만 들어오는 걸 관찰 → virtual call을 Dog.sound()로 inline
```

이후 Cat이 들어오면 **가정 깨짐** → `uncommon trap` 발생 → **interpreter로 복귀 (deoptimization)** → 재프로파일링 후 다시 compile.

이게 **"made not entrant"**의 원인 중 하나.

---

## 4부. JRockit · IBM J9 — 역사와 접근법

#### JRockit (BEA → Oracle, 현재 소멸)

- BEA Systems 개발 → 2008 Oracle 인수 → **JDK 8부터 HotSpot과 통합** (사실상 종료)
- 서버 지향 JVM. "실시간" 응답 보장 (Deterministic GC)
- HotSpot과 다른 특징: **interpreter 없음, 모든 메서드를 즉시 컴파일**

**JRockit의 JIT 컴파일 및 최적화 절차** (책 기반 + 공개 자료):

```
① Method 호출 → 즉시 컴파일 (interpreter 없음)
    ↓
② Code generation — 최소 최적화로 컴파일 (빠른 start)
    ↓
③ Code sampling — 실행 중 어느 메서드가 hot인지 측정
    ↓ (CPU time 많이 쓰는 메서드 발견)
④ Code hotspot optimization — hot 메서드를 **더 공격적 최적화로 재컴파일**
    ↓
⑤ 재배포 — 실행 중인 메서드를 **in-place로 새 버전으로 교체**
```

**JDK 25 (2026) 현재 — 같은 메커니즘인가?**

**아니오. JRockit은 종료.** 하지만 **"hot code를 추적해서 재최적화"**라는 **철학은 HotSpot에 통합**됨:

| 개념 | JRockit | 현재 HotSpot (JDK 25) |
|------|---------|---------------------|
| 초기 실행 | 즉시 컴파일 (interpreter 없음) | Interpreter 시작 |
| Hot 감지 | Code sampling | Invocation counter + back-edge counter |
| 재최적화 | Hotspot optimization | Tier escalation (C1 → C2) |
| 재배포 | In-place replace | OSR + make not entrant |
| 다른 레벨 | 2단계 (일반/최적화) | 5단계 (Tier 0~4) |
| Interpreter 사용 | 없음 | 있음 (startup/cold path) |
| 추가 메커니즘 | — | Deoptimization, Code cache 등 |

→ **"concept은 동일, implementation은 더 정교해짐"**. HotSpot이 승자가 된 이유 중 하나는 **tiered compilation**과 **deoptimization**으로 더 세밀하게 조절 가능하기 때문.

#### IBM J9 (현재 Eclipse OpenJ9)

IBM의 JVM. 2017년 Eclipse 재단에 기부되어 OpenJ9으로 재탄생. 특히 **cloud native**, **container** 환경에 최적화.

**IBM JVM JIT의 5가지 최적화 레벨**:

| 레벨 | 이름 | 특징 |
|------|------|------|
| 1 | **cold** | 최소 최적화. 거의 바로 컴파일 |
| 2 | **warm** | 가벼운 최적화 (inlining 일부, dead code 제거) |
| 3 | **hot** | 적극적 최적화 (escape analysis, loop opt) |
| 4 | **very hot** (sampling) | profile 수집 후 재최적화 |
| 5 | **scorching** | 가장 공격적 (SIMD, aggressive inlining, speculation) |

HotSpot의 Tier 1~4와 비슷한 개념이지만 **더 세분화**. 각 레벨은 CPU 비용과 품질의 trade-off.

**OpenJ9의 차별점**:
- **Shared Class Cache** — JIT 결과를 디스크에 저장 → 재시작 시 재사용 → **startup 매우 빠름**
- **Dynamic AOT** — 짧게 실행되는 애플리케이션용 Ahead-of-Time + JIT 혼합
- Container-aware memory (`-XX:+UseContainerSupport` 기본값)

OpenJ9은 OpenJDK가 아닌 **대체 JVM**. 사용하려면 `jdk.openjdk.org` 대신 `www.eclipse.org/openj9/`에서 다운로드.

---

## 5부. JVM 시작/종료 절차

#### 시작 (Startup)

```
① OS가 java launcher 실행 (java.exe / java)
   │
   ├─ JVM 공유 라이브러리 로드 (libjvm.so / jvm.dll)
   │
② HotSpot 초기화
   │
   ├─ Native memory 할당 (heap, Metaspace, Code Cache 등)
   ├─ GC subsystem 초기화 (default: G1)
   ├─ Thread subsystem 초기화 (main thread 생성)
   ├─ Signal handler 등록 (SIGSEGV, SIGPIPE 등)
   ├─ JIT compiler thread 시작 (C1/C2 백그라운드)
   │
③ Bootstrap ClassLoader 초기화
   │
   ├─ 핵심 클래스 로드: java.lang.Object, Class, String, Thread, System...
   ├─ java.base 모듈 초기화
   │
④ Platform ClassLoader → System ClassLoader 체인 설정
   │
   ├─ Classpath/모듈패스 처리
   │
⑤ Main 클래스 로드 (사용자가 지정한 클래스)
   │
   ├─ 이름 resolution: com.example.MyApp
   ├─ loading → linking → initialization (★ 5부에서 상세)
   │
⑥ main() 메서드 호출
   │
   ├─ 바이트코드 실행 (interpreter 모드 시작)
   ├─ 호출 횟수 → threshold 도달 → JIT 컴파일 (백그라운드)
   │
⑦ 사용자 애플리케이션 실행 중
```

#### 종료 (Shutdown)

**정상 종료 조건** (둘 중 하나):
- `main()` 메서드 리턴 + 모든 **non-daemon thread** 종료
- 누군가 `System.exit(code)` 호출

**종료 절차**:
```
① main() 종료 or System.exit() 호출
   │
② Shutdown Hook 실행
   │   Runtime.getRuntime().addShutdownHook(thread) 등록된 것들
   │   병렬 실행됨 (순서 보장 X)
   │   JDK 9+ Spring Boot: SmartLifecycle 활용
   │
③ Non-daemon thread 종료 대기
   │   아직 살아있는 non-daemon이 있으면 그들이 끝날 때까지 기다림
   │   (System.exit은 이걸 skip하고 강제 종료)
   │
④ Finalizer 실행 (JDK 18까지, 그 후 deprecated/제거)
   │   JDK 9+ 권장: Cleaner API
   │
⑤ GC 마지막 실행 (optional, 보통 skip)
   │
⑥ JVM shutdown
   │   Memory 해제, 파일 디스크립터 close
   │   OS에 exit code 반환
```

**비정상 종료**:
- `kill -9` (SIGKILL) — shutdown hook **실행 안 됨**
- OOM 종료 — shutdown hook 실행 됨 (약간의 메모리 여유 있으면)
- `Runtime.halt(code)` — hook 건너뛰고 즉시 종료

**실무 Tip**: Spring Boot는 `ApplicationContext.close()`를 shutdown hook에 등록 → bean destroy / `@PreDestroy` 호출. Graceful shutdown 지원 (`server.shutdown=graceful`).

---

## 6부. 클래스 로딩 절차

JVM 스펙 §5에 정의된 3단계: **Loading → Linking → Initialization**.

```
┌──────────┐    ┌───────────────────────────────┐    ┌──────────────┐
│ Loading  │ →  │           Linking              │ →  │ Initialization│
│          │    │ ① Verify  ② Prepare  ③ Resolve│    │              │
└──────────┘    └───────────────────────────────┘    └──────────────┘
```

#### ① Loading (로딩)

- ClassLoader가 `.class` 파일 (바이트) 읽음
- 바이트 → JVM 내부 메타데이터로 변환 (Klass 구조, Metaspace에 저장)
- `java.lang.Class` mirror 객체 heap에 생성

**ClassLoader 계층** (parent delegation):
```
Bootstrap ClassLoader (java.base, java.lang.* 등)
    ↑
Platform ClassLoader (java.sql, java.xml 등)
    ↑
System ClassLoader (classpath)
    ↑
Custom ClassLoader (서버 컨테이너, OSGi 등)
```

**parent delegation**: 하위 로더가 먼저 **상위에 요청** → 상위가 없다고 하면 자기가 로드. **"같은 클래스가 두 번 로드되는 걸 방지"**.

#### ② Linking (링킹)

**② (1) Verification (검증)**
- 바이트코드가 JVM 스펙에 맞는지 검증
- Type safety 체크 (int + Object 금지 등)
- Stack map frame 검증
- 외부 바이트코드가 보안 뚫지 못하도록 방어
- **StackMapTable** 사용 (JDK 7+)

**② (2) Preparation (준비)**
- **static 필드에 default value** 할당 (0, 0.0, false, null)
- 아직 `<clinit>`은 실행 안 됨
- `public static final int MAX = 10;` 같은 **컴파일 타임 상수**는 여기서 할당됨

**② (3) Resolution (해석)**
- 심볼릭 참조(`"com/example/Foo"`) → 실제 참조로 변환
- Lazy 수행 가능 (사용되는 시점에)

#### ③ Initialization (초기화)

- **`<clinit>` 메서드 실행**
  - static 필드 초기화 (대입문 실행)
  - static 블록 실행
  - 선언 순서대로

```java
public class Foo {
    static int a = 10;         // ← <clinit>에서 실행
    static {                   // ← <clinit>에서 실행
        System.out.println("init");
    }
    static int b = 20;         // ← <clinit>에서 실행
}
```

- **수퍼클래스 → 서브클래스** 순서
- **인터페이스**는 default method 있을 때만 초기화
- **thread-safe** (JVM이 class당 lock 보유 — "Initialization lock")

#### 초기화 트리거 (언제 시작되나)

JVM 스펙 §5.5 — **다음 중 하나가 발생할 때**:
- `new`로 인스턴스 생성
- static 필드 접근 (단, 컴파일 타임 상수 제외)
- static 메서드 호출
- Reflection (`Class.forName()`)
- 서브클래스 초기화 → 수퍼클래스 자동 초기화
- `main()` 메서드 포함 클래스 (JVM 시작 시)

**초기화되지 않는 경우** (loading만 수행):
- `Class.forName(name, false, loader)` — initialize=false
- `.class` 리터럴 — e.g., `Foo.class` (loading만, 초기화 X)
- Final 상수 접근 (`Foo.CONSTANT`) — compile 시 inline됨

---

## 7부. JVM 예외 처리 절차

#### `throw`의 바이트코드

```java
public void foo() {
    try {
        bar();
    } catch (RuntimeException e) {
        log(e);
    }
}
```

**바이트코드 (javap -v)**:
```
public void foo();
  Code:
     0: aload_0
     1: invokevirtual #2  // Method bar
     4: goto     14
     7: astore_1          // ← catch 블록 진입점
     8: aload_0
     9: aload_1
    10: invokevirtual #3  // Method log
    14: return
  Exception table:
     from    to  target type
         0     4     7   Class RuntimeException
```

**Exception Table이 핵심**:
- `from`~`to`: try 블록 범위 (PC 기준)
- `target`: catch 블록 시작 PC
- `type`: 잡을 예외 타입

#### 예외 발생 시 흐름

```
① athrow 바이트코드 or JVM 내부 예외 발생
   │
② 현재 메서드의 exception table 조회
   │
   ├─ 현재 PC가 "from~to" 범위 안 + 타입 매칭? ──Yes──▶ catch로 jump
   │                                                    │
   │                                                    └─ 예외 객체는 operand stack top에
   │
③ 매칭 안 됨 → 현재 stack frame POP
   │
④ 호출자(caller)의 PC로 복귀 → ② 반복
   │
⑤ 계속 올라가서 main()까지 도달
   │
   ├─ Thread.UncaughtExceptionHandler가 있으면 호출
   ├─ 없으면 기본 핸들러 → 스택 트레이스 출력
   └─ 그 thread 종료
      │
      ├─ non-daemon이고 다른 non-daemon 없으면 → JVM 종료
      └─ daemon이거나 다른 non-daemon 있으면 → 그 thread만 종료
```

#### Stack Trace 수집 비용

```java
try {
    throw new RuntimeException("x");
} catch (RuntimeException e) {
    // e.getStackTrace() ← 비용 발생
}
```

- 예외 객체 생성 시 **native 코드로 현재 stack 전체 스캔**해서 StackTraceElement[] 생성
- stack 깊이에 비례. 서버 환경에선 100+ depth 흔함
- **매 호출당 수 μs ~ 수십 μs**
- 성능 민감 경로에서 예외를 flow control로 쓰면 치명적

**최적화 옵션** — 스택 트레이스 안 쓸 거면:
```java
public class FastException extends RuntimeException {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;   // ← 스택 수집 skip
    }
}
```
→ 수십 배 빠름. 단 디버깅 시 trace 없어서 불편.

#### 바이트코드 레벨 비용

예외 미발생 시 (happy path) — try-catch 자체 비용은 **거의 0**:
- Exception table lookup은 **예외가 실제 던져질 때만**
- JIT이 try-catch를 포함한 코드 그대로 최적화 가능

예외 발생 시:
- Exception 객체 할당 (heap)
- Stack trace 수집
- Exception table 조회
- Stack unwind
- 모두 합치면 **수 μs ~ 수백 μs**

→ **예외는 "예외적 상황"에만 쓸 것**. control flow (while loop 종료, Optional 대체 등) 금지.

---

## 참고 자료

- JVM Specification §5 — "Loading, Linking, and Initializing" ([docs.oracle.com](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html))
- JVM Specification §2.11 — "Instruction Set Summary" (bytecode exception handling)
- HotSpot internals: [OpenJDK HotSpot](https://openjdk.org/groups/hotspot/)
- Eclipse OpenJ9 — [eclipse.org/openj9](https://www.eclipse.org/openj9/)
- JDK Tool: `java -XX:+PrintCompilation` — JIT 로그
- JDK Tool: `java -Xlog:class+load=info` — 클래스 로딩 로그
