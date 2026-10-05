### Story 05. 지금까지 사용하던 for 루프를 더 빠르게 할 수 있다고?

## 1부. if-else vs switch — 컴파일 관점

#### 바이트코드 핵심 차이

| 패턴 | 바이트코드 명령 | 시간 복잡도 |
|------|---------------|-----------|
| `switch` (dense) | `tableswitch` | **O(1)** — jump table 인덱싱 |
| `switch` (sparse) | `lookupswitch` | **O(log n)** — binary search |
| if-else 체인 | `if_icmpne` × n | **O(n)** — 선형 비교 |

#### 실측 — 같은 로직, 세 가지 바이트코드 (JDK 21 verified)

같은 매핑 (1→10, 2→20, 3→30, 4→40, 5→50)을 세 가지 방식으로 작성:

**① dense switch** — `case 1 ~ case 5`
```
0: iload_0
1: tableswitch   { // 1 to 5
             1: 36
             2: 39
             3: 42
             4: 45
             5: 48
       default: 51
  }
```
→ 입력 x에 대해 **단 한 번의 산술**로 jump 주소 계산 (`base + (x - lo) * entry_size`). 그 다음 바로 jump.

**② sparse switch** — `case 1, case 100, case 10000`
```
0: iload_0
1: lookupswitch  { // 3
             1: 36
           100: 39
         10000: 42
       default: 45
  }
```
→ 정렬된 case 리스트에서 **이진 탐색**. O(log n).

**③ if-else 체인**
```
 0: iload_0
 1: iconst_1
 2: if_icmpne     8
 5: bipush        10
 7: ireturn
 8: iload_0
 9: iconst_2
10: if_icmpne     16
...
```
→ x를 1, 2, 3, 4, 5와 **순서대로 하나씩 비교**. 평균 n/2번, 최악 n번.

#### javac이 tableswitch vs lookupswitch 어떻게 선택하는가

javac은 case 값들의 **밀도**를 기준으로 결정:
- **밀집한 경우** (hi - lo + 1 이 case 수와 가까우면) → `tableswitch`
  - jump table의 크기 = `hi - lo + 1`. 비어있는 슬롯은 default로 jump.
- **희소한 경우** → `lookupswitch` (jump table 낭비 피함)

실측 결과:
| case 값 | 결과 | 설명 |
|---------|------|------|
| 1, 2, 3, 4, 5 | tableswitch | 밀도 100% |
| 1000000, 1000001, 1000002 | **tableswitch** | 값이 크지만 밀도 100% |
| 1, 100, 10000 | lookupswitch | 범위 10000, case 3개 → 밀도 0.03% |
| 1, 2, 3, 1000 | lookupswitch | 범위 1000, case 4개 → 밀도 0.4% |

핵심: **값의 크기(절대값)는 무관. 중요한 건 범위 대비 밀도.**

#### 소스 코드상 case 순서는 무관 — javac이 정렬

```java
// 소스
switch (x) {
    case 5: return 50;
    case 1: return 10;
    case 3: return 30;
    case 2: return 20;
    case 4: return 40;
}
```

컴파일 결과:
```
tableswitch   { // 1 to 5
         1: ...   // ← 정렬됨
         2: ...
         3: ...
         4: ...
         5: ...
}
```

→ **javac이 case를 값 기준으로 재배열해서 jump table 생성.** 소스에서 어떤 순서로 쓰든 바이트코드는 동일.

#### "낮은 숫자부터 작성하면 좋다"는 조언의 실체

컴파일 레벨에서 **소스 작성 순서는 전혀 영향 없음** (위 verify됨). 그럼에도 이 조언이 책에 등장한다면 가능성:

1. **"낮은 숫자"가 아니라 "밀집된 값"을 쓰라는 의미였을 가능성** — 작가의 의도가 "0~N 같은 작은 연속 범위 선호"였다면 결과적으로 tableswitch가 되긴 함. 하지만 핵심은 "낮다"가 아니라 "**연속/밀집**"임. `1000000~1000003`도 tableswitch로 변환됨.
2. **if-else로 머릿속에서 번역해서 조언한 것일 수 있음** — if-else라면 매칭 확률 높은 case를 앞에 두는 게 평균 성능에 유리. switch에선 무관.
3. **당시 (2008년, JDK 5~6) 특정 JVM 구현에서의 미세한 차이** — 가능성 낮음. tableswitch 자체는 JVM 스펙에 명시된 명령.

**실무 결론**: 소스 case 순서 신경 쓸 필요 없음. 집중할 포인트는:
- 가능하면 **case 값을 연속/밀집**시킬 것 → tableswitch 유도
- enum이나 상수 설계 시, 값 범위가 벌어지지 않게

#### if-else를 쓸 때의 유의점

if-else는 **소스 순서대로 평가**되므로 **매칭 확률 높은 조건을 앞에** 두는 게 평균 성능에 유리:
```java
// Bad ❌ — rare case를 먼저 검사
if (user.isAdmin()) { ... }       // 1% 확률
else if (user.isGuest()) { ... }  // 10% 확률
else { ... }                      // 89% 확률 — 평균적으로 2번 비교 후 도달

// Good ✅ — 자주 매칭되는 조건부터
if (user.isNormal()) { ... }      // 89% 확률 — 평균 1번 비교
else if (user.isGuest()) { ... }
else { ... }
```

switch로 바꿀 수 있는 경우 (case 값이 int/String/enum/상수)엔 switch 선호.

---

## 2부. 반복문 내 불필요한 반복 제거

#### 책의 안티패턴 — `toArray()`를 for 안에서

```java
TreeSet<Integer> treeSet2 = ...;
long sum = 0;
for (int i = 0; i < treeSet2.size(); i++) {
    Integer x = (Integer) treeSet2.toArray()[i];   // ⚠️ 매 iteration마다 toArray()
    sum += x;
}
```

**문제점**:
- `toArray()`는 호출마다 **새 `Object[]` 할당 + 전체 원소 복사**. O(n) 비용.
- 루프 n번 돌면서 **매번** 호출 → 총 **O(n²)** 시간 + n개의 임시 배열 생성 (GC 압박).
- n=1000이면 100만 번 비교 + 1000개 임시 배열. 안티패턴 중 가장 흔한 함정.

#### 바이트코드로 확인 — badLoop (JDK 21 verified)

```
 4: iload_3                            ← 루프 조건 체크 시작
 5: aload_0
 6: invokevirtual TreeSet.size:()I     ← ⚠️ 매번 size() 호출
 9: if_icmpge     38
12: aload_0
13: invokevirtual TreeSet.toArray:()[Ljava/lang/Object;   ← ⚠️ 매번 toArray()!
16: iload_3
17: aaload                             ← 방금 만든 배열에서 i번째 꺼냄 → 바로 버림
...
35: goto          4                    ← 루프 시작으로
```

- `invokevirtual TreeSet.toArray` 가 **루프 바디 안**에 있음을 확인.
- 매 iteration마다 새 배열 생성 → 즉시 폐기 → GC 대상.
- `invokevirtual TreeSet.size`도 매 iteration마다 호출됨 (조건 체크용). 이유는 다음 섹션.

#### 왜 매번 `size()`가 호출되는가 — for 루프 의미론 (scope가 아님)

**짧은 답**: Java `for` 루프의 **조건 표현식(condition)은 매 iteration 전에 재평가**되는 것이 Java Language Specification(JLS §14.14.1)에 명시된 동작. "scope가 매번 만들어져서"가 아니라 "루프 의미론이 그렇게 정해져서".

**JLS가 정의하는 for 루프**

소스:
```java
for (int i = 0; i < set.size(); i++) {
    // body
}
```

의미적으로 다음과 동치 (JLS §14.14.1 실행 순서):
```
1. int i = 0;                 // ForInit, 한 번만
2. loop_start:
3.   if (!(i < set.size()))   // ← Expression, 매 pass마다 재평가
4.       goto loop_end;
5.   body;                    // Statement
6.   i++;                     // ForUpdate
7.   goto loop_start;
8. loop_end:
```

조건(`i < set.size()`)이 **매 pass에서 전체 재평가**됨. 그 과정에서 `set.size()` 메서드 호출이 발생.

**scope 관점 — 사용자님 기존 이해 보정**

- for 루프는 **매 iteration마다 새 scope를 만들지 않음**.
- `for (int i = 0; ...)`의 `i`는 **for 블록 전체에 걸쳐 scope가 유지**됨. 매번 재선언/재초기화 X. (JLS §14.14.1: "The scope of a local variable declared by this means is the rest of the ForInit, the Expression, the ForUpdate, and the contained Statement.")
- 루프 body `{ ... }` 내부에서 선언한 지역 변수는 iteration마다 **라이프사이클**은 새로 시작되지만, JVM은 **같은 local variable slot을 재사용**함 (스택 할당/해제 비용 X).
- scope는 "**변수 가시 범위**"에 대한 개념. "**표현식이 몇 번 평가되는지**"와는 다른 축.

→ `size()`가 재호출되는 이유는 **scope 때문이 아니라, "조건 표현식은 매번 다시 평가한다"는 for 루프 의미론 때문**.

**왜 javac이 `size()`를 캐싱하지 않나 — 보수적 안전주의**

이런 코드가 가능하기 때문:
```java
for (int i = 0; i < set.size(); i++) {
    if (shouldRemove(i)) set.remove(...);   // ← 루프 바디에서 size 변경!
}
```

- 루프 바디가 collection을 수정하면 `size()` 값이 바뀜.
- javac이 1회만 평가하면 **원 코드 의미가 바뀜** (경우에 따라 `IndexOutOfBoundsException`, 또는 원소 skip).
- 그래서 javac은 **메서드 호출을 iteration마다 그대로 유지**. 안전 우선.

HotSpot JIT은 escape analysis + method inlining 조합으로 **일부 상황에서 hoisting 가능**하지만:
- hot path (충분히 자주 실행된 코드)만 JIT 적용
- side-effect 분석 어려운 메서드는 포기
- interpreted 모드 (초기 실행)에선 아무 최적화 없음

→ **안전하게 손으로 뽑는 것이 최선**.

**`arr.length`는 왜 괜찮나**

```java
for (int i = 0; i < arr.length; i++) { ... }
```

`arr.length`는 **메서드 호출이 아니라 배열의 field 접근**. 바이트코드로 `arraylength` 명령 1개 (field read, O(1)).
- 배열 자체가 **immutable size**라서 `length` 값은 변할 수 없음.
- 매 iteration마다 접근하긴 하지만 비용이 거의 0.
- 그래서 `goodLoop` 패턴 (`Object[] arr = set.toArray(); for (...; i < arr.length; ...)`)이 성능상 안전.

#### 해결 — loop invariant code motion (루프 불변식 추출)

```java
long sum = 0;
Object[] arr = treeSet2.toArray();     // ✅ 루프 밖에서 한 번만
for (int i = 0; i < arr.length; i++) {
    Integer x = (Integer) arr[i];
    sum += x;
}
```

- `toArray()` 1회 호출로 축소 → O(n²) → O(n).
- 루프 조건도 `arr.length` (배열의 상수 field 접근, O(1)).

#### 바이트코드로 확인 — goodLoop

```
 2: aload_0
 3: invokevirtual TreeSet.toArray      ← ✅ 루프 밖, 1회
 6: astore_3
 7: iconst_0
 8: istore        4
10: iload         4                    ← 루프 조건 시작
12: aload_3
13: arraylength                        ← ✅ 배열의 length field (field load, 메서드 호출 X)
14: if_icmpge     41
17: aload_3
...
38: goto          10
```

- 루프 바디 안엔 `aaload` (배열 인덱스 접근)뿐. 메서드 호출 없음.

#### 왜 javac/JIT이 자동으로 못 뽑아내나

- **javac**: 단순한 1-pass compiler. 소스 → 바이트코드 1:1 변환. **loop invariant hoisting 안 함.**
- **HotSpot JIT**: 가능은 하지만 보수적. **메서드 호출의 side-effect 가능성** 때문에 자동 추출 못 하는 경우 많음. `toArray()`는 내부에서 새 배열을 할당하므로 "불변"으로 판정하기 어려움.
- 결론: **손으로 뽑아야 안전**.

#### 비슷한 안티패턴 모음

```java
// ❌ 매 iteration마다 메서드 호출 (toString, toUpperCase, 등)
for (int i = 0; i < list.size(); i++) {
    log.info("key=" + list.get(i).toString() + " at " + System.currentTimeMillis());
}

// ❌ 매 iteration마다 정규식 컴파일
for (String s : list) {
    if (s.matches("\\d+")) { ... }  // 매번 Pattern.compile!
}
// ✅
Pattern p = Pattern.compile("\\d+");
for (String s : list) {
    if (p.matcher(s).matches()) { ... }
}

// ❌ 매 iteration마다 Collection → List 변환
for (int i = 0; i < set.size(); i++) {
    Object x = new ArrayList<>(set).get(i);
}

// ❌ 루프 안에서 상수 String 연산
for (int i = 0; i < n; i++) {
    String url = "https://api.example.com".toLowerCase() + "/users/" + i;
    // "https://..." + ".toLowerCase()" 결과도 매번 다시 계산되는지 JIT 눈치봄
}
```

#### 체크리스트 — 루프 바디에 들어가면 안 되는 것들

- [ ] 결과가 매번 똑같은 메서드 호출 (`list.toArray()`, `getClass()`, `Pattern.compile()`...)
- [ ] 상수 String 연산 (리터럴 조합)
- [ ] 변경되지 않는 자료구조 재생성 (`new ArrayList<>(set)`)
- [ ] 변경되지 않는 조건의 재계산 (`System.getenv("...")`, `config.get(...)`)

---

## 3부. 추가 — JDK 버전별 switch 기능 확장

성능은 아니고 기능 확장 쪽 변화:

| JDK | 변화 |
|-----|------|
| 1.0 ~ 1.6 | int, char, short, byte, enum만 switch 가능 |
| 7 | **String switch** 추가 (내부적으로 hashCode + equals로 변환) |
| 12~13 | switch expression preview (화살표 문법, 값 반환) |
| 14 | switch expression **정식** |
| 16 | pattern matching for `instanceof` 정식 |
| 21 | **pattern matching for switch 정식** (JEP 441) — 타입 패턴, record 분해 |
| 25 | pattern matching 세부 개선 |

JDK 21+ 예시:
```java
return switch (obj) {
    case Integer i when i > 0 -> "positive int: " + i;
    case String s -> "string: " + s;
    case null -> "null";
    default -> "other";
};
```

내부적으로는 여전히 **invokedynamic + bootstrap**으로 번역됨 (문자열 concat과 같은 방식). tableswitch/lookupswitch 메커니즘은 단순 int/char case에서만 사용.

---

## 참고 자료

- JVM Spec §6.5 `tableswitch`, `lookupswitch` — [docs.oracle.com](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-6.html)
- OpenJDK javac `Lower.visitSwitch` — tableswitch/lookupswitch 선택 로직 (space/time cost 비교)
- JEP 441: Pattern Matching for switch (JDK 21)
