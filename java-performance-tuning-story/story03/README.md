### Story 03. 왜 자꾸 String을 쓰지 말라는 거야?

#### 핵심 개념

- **`String`** — 불변(immutable). 값이 바뀌면 **매번 새 객체**가 heap에 생성되고 기존 객체는 GC 대상이 됨. 내부 저장소: JDK 8까지 `char[]`, JDK 9+부터 `byte[] + coder` (Compact Strings, JEP 254).
- **`StringBuffer`** — 가변(mutable). JDK 1.0부터 존재. 모든 메서드가 `synchronized` → 스레드 안전하지만 lock 오버헤드 있음.
- **`StringBuilder`** — 가변(mutable). JDK 5부터 추가됨. `StringBuffer`와 API 거의 동일하지만 `synchronized` 없음 → 단일 스레드에서 더 빠름. 둘 다 `AbstractStringBuilder`를 상속.

#### 특징 비교

| 항목 | `String` | `StringBuffer` | `StringBuilder` |
|------|----------|----------------|-----------------|
| 가변성 | 불변 | 가변 | 가변 |
| 스레드 안전 | O (불변이라 자연스럽게 안전) | O (`synchronized`) | X |
| 성능 | 낮음 (연산마다 새 객체) | 중간 (lock 오버헤드) | 높음 |
| 도입 버전 | JDK 1.0 | JDK 1.0 | JDK 5 |
| 내부 저장소 (JDK 9+) | `byte[] + coder` | 상속 `AbstractStringBuilder.value: byte[]` | 동일 |
| 기본 용량 | 해당 없음 | 16 chars | 16 chars |

#### 언제 쓸까

- **`String`** — 생성 후 바뀔 일 없는 고정 문자열, 상수, 짧은 리터럴. `+` 연산도 **한 라인에서 한 번만** 쓸 거면 OK (아래 "컴파일 변화" 참고).
- **`StringBuffer`** — 멀티 스레드가 **같은 buffer**를 공유해야 하는 희귀한 경우. 실무에서 거의 안 씀 (대부분 `ConcurrentLinkedQueue` 등 더 적절한 자료구조 선택).
- **`StringBuilder`** — 반복문 안에서 문자열 쌓기, 긴 문자열 조립, 단일 스레드 상황 전부. **기본 선택지**.

#### JDK 버전별 `+` 연산자 컴파일 변화

같은 소스 코드:
```java
public static String concat(String name, int age) {
    return "Hello " + name + ", age " + age + "!";
}
```

javac이 이 `+` 연산자를 **버전별로 다르게 번역**함.

##### JDK 1.0 ~ 1.4 — `StringBuffer` 체인 (스펙 기반)

> ⚠️ 로컬 검증 불가 (JDK 21의 javac은 `--release 7` 이하 미지원). 이하 내용은 Java Language Spec과 javac 소스 코드에서 확인된 공식 동작.

```
new StringBuffer()
dup
invokespecial StringBuffer.<init>()
ldc "Hello "
invokevirtual StringBuffer.append(String)
aload name
invokevirtual StringBuffer.append(String)
...
invokevirtual StringBuffer.toString()
```

- 매 `+`마다 `StringBuffer.append()` 호출
- `StringBuffer`의 `synchronized` 때문에 **lock 오버헤드** 발생
- 단일 스레드에서도 매번 모니터 획득/해제 비용 지불

##### JDK 5 ~ 8 — `StringBuilder` 체인 (verified: `javac --release 8` 출력)

```
 0: new           #7                  // class java/lang/StringBuilder
 3: dup
 4: invokespecial #9                  // Method java/lang/StringBuilder."<init>":()V
 7: ldc           #10                 // String Hello
 9: invokevirtual #12                 // Method StringBuilder.append:(Ljava/lang/String;)Ljava/lang/StringBuilder;
12: aload_0
13: invokevirtual #12                 // append(String)
16: ldc           #16                 // String , age
18: invokevirtual #12                 // append(String)
21: iload_1
22: invokevirtual #18                 // append(I)  ← int 전용 오버로드
25: ldc           #21                 // String !
27: invokevirtual #12                 // append(String)
30: invokevirtual #23                 // toString()
33: areturn
```

- JDK 5에서 `StringBuilder` 도입과 함께 javac이 전환 — **`synchronized` 제거**로 성능 향상
- 하지만 여전히 **매 `+`마다 메서드 호출 + heap allocation (`new StringBuilder()`)**
- 반복문 안에서 `+`를 쓰면 매 iteration마다 `new StringBuilder()` 발생 → 최악

##### JDK 9 ~ 25 — `invokedynamic` + `makeConcatWithConstants` (verified: JDK 21 javac 출력)

```
0: aload_0
1: iload_1
2: invokedynamic #7,  0              // InvokeDynamic #0:makeConcatWithConstants:(Ljava/lang/String;I)Ljava/lang/String;
7: areturn
```

- **JEP 280** (Indify String Concatenation, JDK 9 포함)
- `+` 횟수와 무관하게 `invokedynamic` **한 번**으로 축약
- 바이트코드 자체에서는 Builder/Buffer 관련 코드가 **완전히 사라짐**
- 실제 연결 로직은 런타임에 `java.lang.invoke.StringConcatFactory`가 **동적으로 최적화된 핸들러**를 생성 → JIT이 더 효과적으로 인라인/벡터화 가능
- JDK 9부터 JDK 25까지 **바이트코드 형태는 동일**. 성능 개선은 런타임 (`StringConcatFactory`, JIT) 쪽에서 꾸준히 이뤄짐

#### 세대별 성능 특성 요약

| JDK | 컴파일된 형태 | 특징 |
|-----|-------------|------|
| 1.0 ~ 1.4 | `StringBuffer` 체인 | sync 오버헤드로 느림 |
| 5 ~ 8 | `StringBuilder` 체인 | sync 제거, 그래도 매번 `new` 호출 |
| 9 ~ 25 | `invokedynamic` 호출 | 런타임이 최적 전략 선택, 바이트코드 최소화 |

#### 실측 환경

- 로컬 JDK: Corretto 21.0.9 (2025-10-21)
- 검증 커맨드:
  ```bash
  javac --release 8 StringTest.java   # JDK 8 형태
  javac StringTest.java               # JDK 21 (= 9~25) 형태
  javap -c -p StringTest.class
  ```
- JDK 1.4 바이트코드는 로컬 verify 불가 → JEP 280, Oracle 공식 문서, javac 역사 기반 설명

#### 주의점 / 함정

- **반복문 안의 `+`는 여전히 함정** — JDK 9+에서 `invokedynamic`으로 압축되지만, **매 iteration마다 호출 자체는 발생**. 긴 반복에서는 명시적 `StringBuilder` 재사용이 더 빠름.
  ```java
  // Bad ❌ (매 iteration마다 invokedynamic 호출 + 임시 String 생성)
  String result = "";
  for (String s : list) result += s;

  // Good ✅
  StringBuilder sb = new StringBuilder();
  for (String s : list) sb.append(s);
  String result = sb.toString();
  ```
- **단일 라인의 `+` 체인은 JDK 9+에서 걱정 X** — `"a" + b + "c"` 같은 한 번의 연결은 `invokedynamic` 한 번으로 끝남. 과도한 `StringBuilder` 수동 전환은 가독성만 떨어뜨림.
- **`StringBuffer`를 쓸 이유는 사실상 없음** — 공유 가변 상태는 `synchronized` buffer보다 더 적절한 자료구조 (concurrent collections, 메시지 전달)가 있음.
- **초기 용량 지정**: 최종 길이를 예상할 수 있다면 `new StringBuilder(expectedSize)`로 생성해서 내부 배열 재할당(`Arrays.copyOf`) 횟수 줄이기.
- **`String.intern()` 남용 주의** — 상수 풀에 들어간 문자열은 영구적. 동적 생성 문자열에 intern() 걸면 메모리 누수.

#### 참고 자료

- [JEP 280: Indify String Concatenation](https://openjdk.org/jeps/280)
- [JEP 254: Compact Strings](https://openjdk.org/jeps/254)
- `java.lang.invoke.StringConcatFactory` Javadoc
