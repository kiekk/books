### Story 19. GC 튜닝을 항상 할 필요는 없다.

## 0부. GC 튜닝이 **정말 필요한가** — 책의 핵심 메시지

책 Story 19 제목이 암시하듯, **대부분의 애플리케이션은 기본 설정으로 충분**. GC 튜닝은 다음 경우에만:

**튜닝이 필요한 경우**:
- GC 로그 분석 결과 **GC 시간이 전체의 5% 이상**
- 특정 요청에서 **p99 지연이 SLA 초과**
- Full GC가 **분당 1회 이상** 발생
- OOM 반복 발생
- 응답 시간 요구 사항이 매우 엄격 (금융, 광고 bidding 등)

**튜닝이 불필요한 경우**:
- 전체 CPU 중 GC 비율 < 5%
- Full GC는 가끔 (시간당 1회 이하)
- SLA 충족 중
- 응답 시간이 안정적

**튜닝 전 반드시 할 일 — 코드 검증**:
- 메모리 누수 (static 캐시, ThreadLocal, 리스너 등록)
- 불필요한 큰 객체 생성 (loop 안 new, String concat)
- 과도한 Full GC 트리거 (`System.gc()` 호출)

→ **"튜닝으로 해결할 수 있는 건 코드 레벨의 1/10도 안 됨"**.

---

## 1부. GC 성능을 결정하는 옵션 정리

> 아래 값들은 JDK 21 로컬 verified 기본값.

#### ① Heap 크기 옵션 ⭐ (가장 중요)

| 옵션 | 의미 | 기본값 | 권장 |
|-----|------|------|------|
| **`-Xms<size>`** | 초기 heap | 물리 RAM/64 | **`-Xmx`와 동일하게** |
| **`-Xmx<size>`** | 최대 heap | 물리 RAM/4 (max 32 GB) | 서비스 메모리 명시 |
| `-Xmn<size>` | Young generation size | — | 보통 `-XX:NewRatio`로 조절 |
| `-XX:NewSize` / `-XX:MaxNewSize` | Young 세대 min/max | — | ergonomic 자동 |
| `-XX:NewRatio=N` | **Old/Young 비율** | **2** (Young:Old = 1:2) | 보통 그대로 |
| `-XX:SurvivorRatio=N` | **Eden/Survivor 비율** | **8** (Eden:S0:S1 = 8:1:1) | 보통 그대로 |

**단위**: `k/K` (KB), `m/M` (MB), `g/G` (GB). 예: `-Xmx2g`.

**중요 — `-Xms = -Xmx` 설정 권장**:
- 둘이 다르면 JVM이 **런타임에 heap resize** → 성능 저하
- 운영 환경에선 거의 항상 같게 설정

#### ② GC 종류 선택

| 옵션 | 사용 상황 |
|-----|---------|
| `-XX:+UseSerialGC` | heap < 100MB, single core (CLI 도구) |
| `-XX:+UseParallelGC` | throughput 최대, 지연 상관없음 |
| **`-XX:+UseG1GC`** | **JDK 9+ 기본, 범용** |
| `-XX:+UseZGC` | 저지연 (p99 < 10ms) |
| `-XX:+ZGenerational` | ZGC + Generational (JDK 21+). ZGC와 함께 지정 |
| `-XX:+UseShenandoahGC` | ZGC 대안 (Red Hat JDK 등) |
| `-XX:+UseEpsilonGC` | 벤치마크, 단명 애플리케이션 |

**현재 설정 확인**:
```bash
jcmd <pid> VM.flags | tr ' ' '\n' | grep -E "UseG1GC|UseZGC|UseParallelGC"
```

#### ③ G1 전용 옵션 (JDK 21 기본 GC)

| 옵션 | 의미 | 기본값 | 조정 가이드 |
|-----|------|------|-----------|
| **`-XX:MaxGCPauseMillis=N`** ⭐ | **목표 pause time** (ms) | **200** | 작게 설정하면 Young 작아지고 GC 빈번. **100~500 ms 범위 권장** |
| `-XX:G1HeapRegionSize=N` | region 크기 | **4 MB** (ergonomic, 1~32 MB) | 보통 자동 |
| `-XX:G1NewSizePercent=N` | 최소 Young % | 5 | 거의 조정 안 함 |
| `-XX:G1MaxNewSizePercent=N` | 최대 Young % | 60 | 거의 조정 안 함 |
| **`-XX:InitiatingHeapOccupancyPercent=N`** | **concurrent cycle 트리거** (%) | **45** | heap 압박 시 **낮추면** Full GC 전 미리 cleanup |
| `-XX:G1MixedGCCountTarget=N` | Mixed GC 횟수 target | 8 | — |

#### ④ ZGC 전용 옵션

| 옵션 | 의미 |
|-----|------|
| **`-XX:+UseZGC`** | ZGC 활성화 |
| **`-XX:+ZGenerational`** | Generational ZGC (JDK 21+). **JDK 23+는 기본값** |
| `-XX:ZCollectionInterval=N` | 강제 collection 주기 (초) |
| `-XX:ZAllocationSpikeTolerance=N` | 할당 spike 허용치 |

**ZGC는 설정 단순**. 대부분 `-XX:+UseZGC`만으로 충분. heap만 넉넉히.

#### ⑤ 승격 / 세대 관련

| 옵션 | 의미 | 기본값 | 조정 가이드 |
|-----|------|------|-----------|
| **`-XX:MaxTenuringThreshold=N`** | **Survivor 왕복 횟수** → Old 승격 | **15** (verified JDK 21) | 작게 하면 조기 승격 (Old 압박) |
| `-XX:TargetSurvivorRatio=N` | Survivor 사용률 목표 | **50**% | — |
| `-XX:PretenureSizeThreshold=N` | 바로 Old로 가는 큰 객체 크기 | 0 (G1) | G1에선 region/2 초과 = Humongous |
| `-XX:+PrintTenuringDistribution` | 승격 통계 로그 | false | 튜닝 중일 때 ON |

#### ⑥ Metaspace

| 옵션 | 의미 | 기본값 |
|-----|------|------|
| **`-XX:MetaspaceSize=N`** | 초기 Metaspace | 21 MB (JDK 21) |
| **`-XX:MaxMetaspaceSize=N`** | 최대 Metaspace | **unlimited** (OS 메모리까지) ⚠️ |
| `-XX:MaxMetaspaceFreeRatio` | 축소 트리거 상한 | 70 |
| `-XX:MinMetaspaceFreeRatio` | 확장 트리거 하한 | 40 |

**중요**: 운영 환경에선 `-XX:MaxMetaspaceSize` **반드시 명시** (예: 256m). ClassLoader leak 시 서버 전체 메모리 삼키는 걸 방지.

#### ⑦ 로그 및 디버깅

| 옵션 | 의미 |
|-----|------|
| **`-Xlog:gc*=info:file=gc.log:tags,uptime,time,level:filecount=10,filesize=10m`** | **GC 로그 상시 수집** (JDK 9+) |
| **`-XX:+HeapDumpOnOutOfMemoryError`** | OOM 시 heap dump (verified 기본 **false** ⚠️) |
| `-XX:HeapDumpPath=/path/dumps/` | dump 저장 경로 |
| **`-XX:+ExitOnOutOfMemoryError`** | OOM 즉시 종료 (orchestrator 재시작용) |
| `-XX:+PrintFlagsFinal` | JVM 플래그 전부 출력 (디버깅용) |
| **`-XX:+DisableExplicitGC`** | `System.gc()` 무시 |

#### ⑧ 기타 성능 관련

| 옵션 | 의미 | 기본값 |
|-----|------|------|
| `-XX:+UseCompressedOops` | 64-bit 포인터 압축 (heap < 32GB) | **true** (ergonomic) |
| `-XX:+UseCompressedClassPointers` | 클래스 포인터 압축 | true |
| **`-XX:+AlwaysPreTouch`** | **JVM 시작 시 heap 전체 미리 할당** | **false** (verified) |
| `-XX:+UseLargePages` | OS large page 사용 | false |

**`-XX:+AlwaysPreTouch`** 효과:
- JVM 시작 시간 길어짐 (전체 메모리 touch)
- **런타임 중 메모리 할당 지연 제거** → 응답시간 안정화
- 지연 민감 서비스에서 권장

---

## 2부. GC 튜닝 절차 — 7단계

### 0단계: 튜닝이 **정말 필요한가** 판단

```bash
# GC overhead 측정
jstat -gcutil <pid> 10s
# GCT 컬럼 추적. 애플리케이션 수명 대비 % 환산

# 5% 이상이면 튜닝 검토
# 1% 이하면 튜닝 효과 미미 → 다른 bottleneck 조사
```

### 1단계: **측정** (현재 상태 파악)

**수집할 데이터**:
```bash
# 24~48시간 GC 로그
java -Xlog:gc*=info:file=gc.log:tags,uptime,time,level:filecount=10,filesize=10m

# 애플리케이션 성능
- 응답시간 (p50/p95/p99)
- 처리량 (TPS)
- GC 시간 비율
- Full GC 횟수/주기
- Allocation rate (초당 할당 바이트)
```

**분석 도구**:
- **GCEasy** (gceasy.io) — 자동 분석 리포트
- **GCViewer** — 로컬 분석
- **JFR + JMC** — 더 깊은 분석

### 2단계: **목표 설정** (SLA)

구체적 숫자로 명시:
- 응답시간 p99 < 100 ms
- Full GC 분당 0.5회 이하
- GC overhead < 3%
- OOM 0회

### 3단계: **분석** — 어디가 문제인가

| 증상 | 원인 추측 |
|-----|---------|
| Full GC 많음 | heap 작음 or 메모리 leak |
| Young GC 자주, 짧음 | 정상 or Young 작음 |
| Young GC 긺 | Young 큼 or Survivor 작음 |
| Full GC가 pause 100ms 이상 | heap 큼 or GC 알고리즘 부적합 |
| Metaspace OOM | ClassLoader leak |
| Allocation rate 높음 (수 GB/s) | 비효율 코드 (loop 안 new 등) |

### 4단계: **가설 → 적용** (한 번에 하나씩)

| 가설 | 조치 |
|-----|------|
| "heap이 작아서 Full GC 자주" | `-Xmx` 2배 증가 |
| "Young이 작아서 조기 승격" | `-XX:NewRatio=1` (Young 비율 증가) |
| "G1 pause 길어서 응답지연" | `-XX:MaxGCPauseMillis=100` |
| "응답시간 p99 민감" | ZGC 전환 (`-XX:+UseZGC -XX:+ZGenerational`) |
| "Full GC 전에 cleanup하고 싶다" | `-XX:InitiatingHeapOccupancyPercent=35` (낮춤) |
| "시작 직후 지연" | `-XX:+AlwaysPreTouch` |

**원칙**: **한 번에 한 옵션만** 바꾸고 측정. 안 그러면 원인 추적 불가.

### 5단계: **검증** — 부하 테스트

- 변경 전/후 동일 부하로 비교
- SLA 재측정
- 최소 24시간 모니터링 (하루 중 패턴 변동 체크)

### 6단계: **반복 or 롤백**

- 효과 있으면 운영 환경 반영 (canary → gradual rollout)
- 효과 없거나 악화되면 롤백, 다른 가설

---

## 3부. 흔한 튜닝 패턴 — 상황별 공식

#### 상황 1: 서버 애플리케이션 기본 (JDK 21)

```bash
java \
  -Xms2g -Xmx2g \                    # heap resize 비용 제거
  -XX:MaxMetaspaceSize=256m \        # Metaspace 상한 (leak 방지)
  -XX:+HeapDumpOnOutOfMemoryError \  # OOM 시 dump
  -XX:HeapDumpPath=/data/dumps/ \
  -XX:+ExitOnOutOfMemoryError \      # OOM 후 즉시 종료
  -Xlog:gc*=info:file=/data/logs/gc.log:tags,uptime,time,level:filecount=10,filesize=10m \
  -XX:+DisableExplicitGC \           # System.gc() 무시
  -jar app.jar
```

#### 상황 2: 저지연 서비스 (p99 < 10 ms)

```bash
java \
  -Xms4g -Xmx4g \
  -XX:+UseZGC -XX:+ZGenerational \   # ZGC로 전환
  -XX:+AlwaysPreTouch \              # 시작 시 메모리 미리 할당
  -XX:MaxMetaspaceSize=256m \
  ... (나머지는 상황 1과 동일)
```

#### 상황 3: 배치/ETL (처리량 최대)

```bash
java \
  -Xms8g -Xmx8g \
  -XX:+UseParallelGC \               # throughput 최대
  -XX:ParallelGCThreads=8 \          # GC 스레드 수 (CPU 코어 수)
  -XX:MaxMetaspaceSize=256m \
  ... 
```

#### 상황 4: Full GC가 자주 발생

**단계적 접근**:
1. GC 로그에서 Full GC 직전 상태 확인
2. 메모리 leak 여부 heap dump + MAT 분석
3. leak 없으면 heap 증가 (`-Xmx` 2배)
4. 여전히 많으면 `-XX:InitiatingHeapOccupancyPercent=35`로 미리 cleanup
5. 그래도 많으면 ZGC 고려

#### 상황 5: 메모리 사용량 느리게 증가

```bash
# 1. 매 10분마다 heap 사용량 기록
jstat -gc <pid> 600s 144 > heap_24h.log

# 2. 24시간 후 분석. O (Old used)가 선형 증가하면 leak

# 3. 몇 시간 간격으로 heap dump 2개
jcmd <pid> GC.heap_dump filename=dump1.hprof
# ... 수 시간 뒤
jcmd <pid> GC.heap_dump filename=dump2.hprof

# 4. Eclipse MAT에서 "Compare Baseline" → 어떤 객체가 증가하는지
```

---

## 4부. 안티패턴 모음

| 안티패턴 | 왜 안 좋나 |
|---------|---------|
| **측정 없이 플래그 복붙** | 플래그는 상황별로 다름. 블로그 복붙은 역효과 흔함 |
| **`-Xmx`만 무작정 올리기** | 커지면 Full GC pause 길어짐. GC 알고리즘 선택이 더 중요 |
| **`System.gc()` 호출** | Full GC 트리거. 성능 큰 저하 |
| **`-XX:MaxMetaspaceSize` 설정 안 함** | leak 시 OS 메모리 다 삼킴 → Linux OOM killer |
| **`-Xms ≠ -Xmx`** | 런타임 resize 비용 |
| **여러 플래그 동시 변경** | 어떤 게 효과였는지 모름 |
| **`-XX:+PrintGCDetails` (JDK 9+)** | deprecated. `-Xlog:gc*` 쓸 것 |
| **G1에서 `-Xmn` 설정** | G1은 region 기반. Young 크기는 ergonomic이 더 똑똑 |
| **메모리 leak을 플래그로 해결** | 근본 원인 해결 필요. 플래그는 지연제일 뿐 |

---

## 5부. 책의 Golden Rules 재해석

Story 19가 전하는 핵심:

1. **"GC 튜닝은 최후의 수단"** — 코드/구조 개선이 먼저
2. **"측정 없이 튜닝 금지"** — GC 로그, JFR, APM이 전제
3. **"한 번에 한 변수만"** — 과학 실험의 원칙
4. **"기본 설정이 대부분 잘 설계됨"** — JVM 엔지니어들이 신중히 결정한 값
5. **"운영 환경 변경은 신중히"** — canary, A/B test, 롤백 플랜

→ **진짜 성능 개선의 90%는 애플리케이션 코드에서 나옴**. GC 튜닝은 그 나머지 10%의 일부를 다룰 뿐.

---

## 참고 자료

- [JDK 21 Troubleshooting Guide](https://docs.oracle.com/en/java/javase/21/troubleshoot/)
- [Oracle: Garbage Collection Tuning Guide](https://docs.oracle.com/en/java/javase/21/gctuning/)
- [HotSpot JVM Options](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html#extra-options-for-java)
- [GCEasy](https://gceasy.io/) — 자동 GC 분석
- Shipilëv's "GC Tuning 101" 발표
