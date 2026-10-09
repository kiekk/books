### Story 18. GC가 어떻게 수행되고 있는지 보고 싶다.

## 1부. `jstat` — JVM 상태 커맨드 라인 조회

#### 기본 형식

```bash
jstat -<option> <pid> [interval] [count]
```

- `pid`: Java 프로세스 ID (`jps`로 조회)
- `interval`: 조회 주기 (예: `1s`, `500ms`). 생략 시 1회만 출력
- `count`: 몇 번 출력할지. 생략 시 무한

**실행 예**:
```bash
jps -l                       # Java 프로세스 목록
# 35010 com.example.MyApp

jstat -gcutil 35010 1s 10    # 1초마다 10번 GC 활용률 출력
```

#### 전체 옵션 종류 (JDK 21)

| 옵션 | 용도 |
|-----|-----|
| `-class` | 클래스 로딩/언로딩 통계 |
| `-compiler` | JIT 컴파일 통계 |
| `-gc` | GC **상세** (각 영역 capacity/used 바이트 단위) |
| `-gccapacity` | 각 세대의 min/max/current capacity |
| `-gcnew` | Young generation GC 통계 |
| `-gcnewcapacity` | Young generation capacity |
| `-gcold` | Old generation GC 통계 |
| `-gcoldcapacity` | Old generation capacity |
| `-gcmetacapacity` | Metaspace capacity |
| `-gcutil` ⭐ | GC **활용률 요약** (가장 자주 쓰임) |
| `-gccause` | 마지막 GC + 현재 GC의 **원인** |
| `-printcompilation` | JIT compilation 상세 |

---

## 2부. 각 옵션 출력 해석

> 아래 출력은 JDK 21에서 실제 실행한 결과.

#### `-gcutil` — 가장 자주 쓰는 요약

```
  S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT       GCT
     -      -   0.00   0.00      -      -      0     0.000     0     0.000     0     0.000     0.000
```

**컬럼 의미**:

| 컬럼 | 의미 | 해석 |
|-----|------|------|
| **S0** | Survivor 0 사용률 (%) | 80~90% 지속 → Young GC 임박 or 승격 많음 |
| **S1** | Survivor 1 사용률 (%) | S0과 번갈아가며 사용 |
| **E** | Eden 사용률 (%) | 100% 도달 시 Young GC 발동 |
| **O** | **Old generation 사용률 (%)** ⭐ | 꾸준히 증가 → leak 의심. 90%+ → Full GC 임박 |
| **M** | Metaspace 사용률 (%) | 95%+ → ClassLoader leak 의심 |
| **CCS** | Compressed Class Space 사용률 (%) | class metadata 압축 영역 |
| **YGC** | Young GC **누적 횟수** | 급증하면 Young 압박 |
| **YGCT** | Young GC **누적 시간** (초) | 요청 응답에 영향 |
| **FGC** | **Full GC 누적 횟수** ⭐ | **분당 1회 이상이면 심각** |
| **FGCT** | Full GC 누적 시간 (초) | 각 Full GC는 수백 ms~수 초 |
| **CGC** | Concurrent GC 횟수 (G1/ZGC) | G1의 concurrent cycle |
| **CGCT** | Concurrent GC 총 시간 | STW 아닌 백그라운드 수행 |
| **GCT** | 전체 GC 총 시간 (초) | 애플리케이션 전체 실행 시간 대비 비율로 환산 |

**ZGC/Shenandoah의 경우** `S0/S1/E/M/CCS`가 `-`로 표시됨 — 세대 구분 안 하거나 다른 메모리 모델이기 때문.

#### `-gc` — 바이트 단위 상세

```
    S0C      S1C     S0U     S1U       EC         EU         OC         OU        MC      MU    CCSC   CCSU   YGC  YGCT  FGC  FGCT  CGC  CGCT  GCT
     0.0      0.0     0.0     0.0  20480.0        0.0   274432.0        0.0     0.0     0.0   0.0    0.0    0 0.000    0 0.000    0 0.000 0.000
```

**C = Capacity, U = Used** 접미사:

| 컬럼 | 의미 |
|-----|------|
| S0C / S0U | Survivor 0 capacity / used (KB) |
| EC / EU | Eden capacity / used |
| OC / OU | Old capacity / used |
| MC / MU | Metaspace capacity / used |
| CCSC / CCSU | Compressed Class Space capacity / used |

→ **"지금 Old에 몇 KB 쓰고 있나?"**를 바이트 단위로 확인 가능.

#### `-gccause` — 왜 GC가 발생했나

```
S0     S1     E      O      M     CCS    YGC     YGCT     FGC    FGCT     CGC    CGCT     GCT    LGCC                 GCC
   -      -   0.00   0.00      -      -      0     0.000     0     0.000     0     0.000  0.000  No GC                No GC
```

추가 컬럼:
- **LGCC** (Last GC Cause): 직전 GC 원인
- **GCC** (Current GC Cause): 현재 진행 중인 GC 원인

**흔한 원인값**:
| 원인 | 의미 |
|-----|------|
| `No GC` | GC 안 돌고 있음 |
| `Allocation Failure` | Eden 꽉 참 → Young GC |
| `Metadata GC Threshold` | Metaspace 임계 → Metaspace 수거 |
| `Ergonomics` | JVM 자동 판단 |
| `System.gc()` | 애플리케이션이 명시 호출 (⚠️ 보통 안티패턴) |
| `G1 Evacuation Pause` | G1의 Young/Mixed GC |
| `G1 Humongous Allocation` | Humongous object (>region/2) 할당 |
| `GCLocker Initiated GC` | JNI critical section 해제 후 |

→ **갑자기 Full GC가 많이 발생**할 때 LGCC로 원인 추적.

#### `-gccapacity` — 각 영역의 크기 한계

```
NGCMN   NGCMX   NGC    S0C   S1C    EC      OGCMN   OGCMX   OGC    OC     MCMN  MCMX    MC   CCSMN  CCSMX   CCSC  YGC  FGC  CGC
   0.0 4718592.0 20480.0 0.0 0.0 20480.0  0.0 4718592.0 274432.0 274432.0  0.0 1048576.0 0.0  0.0 1048576.0  0.0   0    0    0
```

- **NGCMN / NGCMX / NGC**: New gen Min / Max / Current capacity (KB)
- **OGCMN / OGCMX / OGC**: Old gen Min / Max / Current
- **MCMN / MCMX / MC**: Metaspace Min / Max / Current
- **CCSMN / CCSMX / CCSC**: Compressed Class Space Min / Max / Current

→ **"heap을 늘릴 여유가 얼마나 있나"** 확인. `OGCMX`가 `-Xmx`에 반영됨.

#### `-class` — 클래스 로딩 통계

```
Loaded  Bytes  Unloaded  Bytes     Time
   444  1124.8        0     0.0       0.00
```

| 컬럼 | 의미 |
|-----|------|
| Loaded | 로드된 클래스 수 |
| Bytes | 로드된 바이트 (KB) |
| Unloaded | 언로드된 수 |
| Bytes (unloaded) | 언로드된 바이트 |
| Time | 로딩 총 시간 (초) |

→ **ClassLoader leak 조짐** 확인. Loaded는 올라가는데 Unloaded 안 올라가면 의심.

#### `-compiler` — JIT 컴파일 통계

```
Compiled Failed Invalid   Time   FailedType FailedMethod
     143      0       0    0.12            0
```

| 컬럼 | 의미 |
|-----|------|
| Compiled | 컴파일된 메서드 수 |
| Failed | 컴파일 실패 수 |
| Invalid | invalid 처리된 수 |
| Time | 컴파일 총 시간 (초) |

---

## 3부. 실무 모니터링 패턴

#### ① 실시간 모니터링 (매초 체크)

```bash
jstat -gcutil <pid> 1s
```

**정상 상태 (예시)**:
```
  S0     S1     E      O      M     CCS   YGC  YGCT  FGC  FGCT  GCT
  0.00  25.30  45.20  32.19  94.98 92.70   12  0.123   0  0.000 0.123
  0.00  25.30  67.50  32.19  94.98 92.70   12  0.123   0  0.000 0.123
  0.00  25.30  89.10  32.19  94.98 92.70   12  0.123   0  0.000 0.123
 42.10   0.00   5.20  34.80  94.98 92.70   13  0.145   0  0.000 0.145  ← Young GC 발동
```

- E가 올라가다가 → 100% 근처 → **YGC 1 증가**, E 리셋, Survivor 교체 (S0/S1 변경)
- O 안정 → heap 상태 양호

**문제 상황 (leak 의심)**:
```
  S0     S1     E      O      M     CCS   YGC  YGCT  FGC  FGCT  GCT
  0.00  87.10  12.00  65.20  94.98 92.70  120  5.230   0  0.000 5.230
  0.00  87.10  12.00  72.30  94.98 92.70  121  5.280   0  0.000 5.280
  0.00  87.10  12.00  79.80  94.98 92.70  122  5.330   0  0.000 5.330  ← O 꾸준 상승
  0.00  87.10  12.00  85.10  94.98 92.70  123  5.380   0  0.000 5.380
  0.00  87.10  12.00  92.00  94.98 92.70  124  5.430   0  0.000 5.430
  0.00  87.10  12.00  99.00  94.98 92.70  124  5.430   1  2.350 7.780  ← Full GC 발동
  0.00  87.10  12.00  95.00  94.98 92.70  124  5.430   1  2.350 7.780  ← Full GC 후도 95%!
```

→ Full GC 후에도 O가 안 떨어지면 **진짜 메모리 leak**. heap dump 필요.

#### ② Full GC 횟수 감시

```bash
jstat -gcutil <pid> 60s | awk '{print strftime("%H:%M:%S"), $0}'
```

→ FGC 컬럼 추적. **분당 1회 이상이면 경보**.

#### ③ 원인 추적

Full GC가 발생했다면:
```bash
jstat -gccause <pid>
```

LGCC가 `System.gc()`면 → 애플리케이션 코드가 명시 호출 중. 금지:
```bash
java -XX:+DisableExplicitGC -jar app.jar
```

---

## 4부. GC 분석 도구 생태계

#### ① JDK 내장 CLI

| 도구 | 용도 |
|------|------|
| `jps` | Java 프로세스 리스트 |
| `jstat` | 커맨드 라인 통계 (위 상세) |
| `jmap` | heap histogram, heap dump |
| `jcmd` ⭐ | 통합 명령 (modern 권장) |
| `jstack` | thread dump |
| `jinfo` | JVM 플래그/시스템 프로퍼티 조회 |

**`jcmd` 주요 명령어** (jstat 대체 추세):

```bash
jcmd <pid> help                       # 가능한 명령어 목록
jcmd <pid> GC.heap_info               # heap 현재 상태 (verified)
jcmd <pid> GC.class_histogram         # 클래스별 인스턴스 수
jcmd <pid> GC.heap_dump filename=heap.hprof   # heap dump
jcmd <pid> GC.run                     # System.gc() 유발 (디버깅용)
jcmd <pid> VM.classloader_stats       # ClassLoader별 메타스페이스 사용량
jcmd <pid> Thread.print               # thread dump
jcmd <pid> VM.flags                   # JVM 플래그 전부
jcmd <pid> VM.system_properties       # 시스템 프로퍼티
```

**verified GC.heap_info 출력** (JDK 21):
```
garbage-first heap   total 299008K, used 3104K [0x00000006e0000000, 0x0000000800000000)
  region size 4096K, 1 young (4096K), 0 survivors (0K)
 Metaspace       used 82K, committed 320K, reserved 1114112K
  class space    used 6K, committed 128K, reserved 1048576K
```

→ G1의 region 단위 상태까지 상세히 보여줌.

#### ② GC Log 수집

```bash
# JDK 9+ Unified Logging
java -Xlog:gc*=info:file=gc.log:tags,uptime,time,level:filecount=10,filesize=10m -jar app.jar
```

출력 예:
```
[0.123s][info][gc        ] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 24M->5M(100M) 2.345ms
[1.234s][info][gc        ] GC(1) Pause Young (Normal) (G1 Evacuation Pause) 28M->8M(100M) 2.123ms
[5.678s][info][gc        ] GC(2) Pause Full (System.gc()) 50M->15M(100M) 123.456ms
```

**해석**:
- `Pause Young` = Minor GC, `Pause Full` = Full GC
- `24M->5M(100M)`: GC 전 사용량 → GC 후 사용량 (총 heap 크기)
- `2.345ms`: STW 시간

#### ③ GUI 분석 도구

**VisualVM**
- Oracle, 과거엔 JDK에 내장 (JDK 9+에서 분리)
- Plugin: **Visual GC** — 세대별 상태 실시간 시각화
- Download: [visualvm.github.io](https://visualvm.github.io/)

**JDK Mission Control (JMC)**
- Oracle, **JFR (Java Flight Recorder)** 통합
- 가장 강력. heap dump + CPU profile + GC 분석 통합
- JDK 11+에서 **무료** (상용 → OpenJDK 이관)
- JFR 저오버헤드 (<1%) — **운영 환경에서도 상시 활성화 가능**

```bash
java -XX:StartFlightRecording=duration=60s,filename=rec.jfr -jar app.jar
# 수집된 rec.jfr을 JMC로 열어서 분석
```

**JConsole**
- JDK 내장, GUI, 간단한 메트릭
- JMX 기반. 레거시 느낌이지만 가볍고 즉시 사용 가능

#### ④ GC 로그 분석 전용 도구

**GCViewer** (오픈소스)
- GC 로그 파일 로드 → 시간대별 그래프
- `.log` 파일 열기만 하면 됨. 가볍다.
- Github: `chewiebug/GCViewer`

**GCEasy** (웹 기반, 상용)
- [gceasy.io](https://gceasy.io/)에 GC 로그 업로드
- 자동 분석 보고서 생성 (전체 메모리 패턴, Full GC 원인, 추천 등)
- 무료 티어 있음

**PerfMa** (상용)
- 깊은 분석 + 중국어/영어 UI

#### ⑤ Heap Dump 분석

```bash
# dump 생성
jcmd <pid> GC.heap_dump filename=heap.hprof

# 또는 OOM 시 자동 dump
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dumps/
```

**분석 도구**:
- **Eclipse MAT (Memory Analyzer Tool)** ⭐ — 사실상 표준
  - Leak Suspects 리포트
  - Dominator Tree
  - OQL (Object Query Language)
  - [eclipse.dev/mat](https://eclipse.dev/mat/)
- **VisualVM heap walker** — 가벼운 확인
- **IntelliJ IDEA Ultimate** heap viewer

---

## 5부. 시나리오별 Workflow

| 증상 | Workflow |
|-----|---------|
| **응답 느림, 가끔 긴 지연** | ① `jstat -gcutil 1s`로 GC 패턴 확인 → ② FGC 많으면 GC 로그 수집 → ③ GCEasy로 분석 |
| **OOM: Java heap space** | ① heap dump 자동 수집 설정 → ② OOM 발생 시 dump → ③ Eclipse MAT로 Leak Suspects 확인 |
| **OOM: Metaspace** | ① `jcmd VM.classloader_stats` → ② 어떤 ClassLoader가 과다 → ③ Spring Boot 재배포? CGLIB 생성? 조사 |
| **CPU 급증** | ① `jstack`으로 thread dump → ② `top -H`로 Java thread별 CPU → ③ TID를 hex로 변환해서 thread dump 매칭 |
| **점진적 메모리 증가** | ① JFR 1시간 수집 → ② JMC로 메모리 증가 추이 분석 → ③ 특정 클래스 지목 후 코드 리뷰 |
| **GC 튜닝 필요** | ① GC 로그 1일 수집 → ② GCEasy 분석 → ③ MaxGCPauseMillis / 세대 비율 조정 |

#### 운영 환경 상시 세팅 (권장)

```bash
java \
  -Xms2g -Xmx2g \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/dumps/ \
  -XX:+ExitOnOutOfMemoryError \
  -Xlog:gc*=info:file=/data/logs/gc.log:tags,uptime,time,level:filecount=10,filesize=10m \
  -XX:StartFlightRecording=disk=true,maxsize=512m,maxage=1d,filename=/data/jfr/ \
  -jar app.jar
```

- `-Xms = -Xmx`: resize 비용 제거
- `HeapDumpOnOutOfMemoryError`: OOM 즉시 dump
- `ExitOnOutOfMemoryError`: OOM 후 즉시 종료 (orchestrator가 재시작)
- GC log + JFR 모두 디스크 상시 수집

---

## 참고 자료

- [jstat man page (JDK 21)](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jstat.html)
- [jcmd man page](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jcmd.html)
- [JDK Mission Control](https://www.oracle.com/java/technologies/jdk-mission-control.html)
- [Eclipse MAT](https://eclipse.dev/mat/)
- [GCEasy](https://gceasy.io/) — 웹 기반 GC 분석
- Unified JVM Logging — [JEP 158](https://openjdk.org/jeps/158)
