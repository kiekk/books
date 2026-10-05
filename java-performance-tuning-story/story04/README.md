### Story 04. 어디에 담아야 하는지…

#### 왜 ArrayList, HashSet, HashMap이 "대표 구현체"인가

`List`, `Set`, `Map`은 인터페이스. 구현체는 많지만 **실무 90% 이상이 이 셋**에 수렴.

| 인터페이스 | 대표 구현체 | 왜 대표인가 |
|-----------|-----------|-----------|
| `List` | `ArrayList` | 랜덤 접근 O(1), 메모리 locality 좋음, append amortized O(1). `LinkedList`는 노드마다 24~40 bytes 오버헤드 + 캐시 miss로 거의 항상 느림. |
| `Set` | `HashSet` | 삽입/조회/삭제 평균 O(1). `TreeSet`은 O(log n) + 정렬 비용. 순서 필요 없으면 Hash 계열이 압승. |
| `Map` | `HashMap` | Set과 같은 이유. 사실 `HashSet`도 내부적으로 `HashMap` 재활용. |

---

## ArrayList — 가변 크기 배열

#### 내부 구조

```java
transient Object[] elementData;   // 실제 저장소
private int size;                 // 사용 중인 원소 개수 (elementData.length와 다름)
```

- `elementData.length` = **capacity** (할당된 배열 크기)
- `size` = **실제 담긴 원소 수**
- 초기 capacity: **10** (verified: `ArrayList.DEFAULT_CAPACITY = 10`)

#### `add()`와 자동 resize

```java
// JDK 21 실제 소스
public boolean add(E e) {
    modCount++;
    add(e, elementData, size);
    return true;
}
private void add(E e, Object[] elementData, int s) {
    if (s == elementData.length)        // ← 배열이 100% 꽉 찬 그 순간
        elementData = grow();
    elementData[s] = e;
    size = s + 1;
}
```

**⚠️ 자주 하는 오해 — "2/3나 70% 찼을 때 확장되는 것 아닌가?"**

**아닙니다.** ArrayList는 **배열이 완전히 꽉 찼을 때만** 확장. 특정 비율(load factor) 기반 선제 확장 로직은 **JDK 어느 버전에도 존재한 적 없음**. 이 오해는 `HashMap`의 load factor 0.75와 혼동한 것일 가능성 높음.

| | ArrayList | HashMap |
|---|-----------|---------|
| 확장 trigger | `size == capacity` (100%) | `size > capacity × 0.75` (75%) |
| 이유 | 랜덤 접근용 배열이라 빈 슬롯이 성능에 영향 없음 | 해시 충돌 확률을 낮추려면 공간 여유 필요 |

JDK 1.2 공개 소스에서도 동일한 trigger (`if (minCapacity > oldCapacity)`, 여기서 `minCapacity = size + 1` → `size >= capacity`일 때만 확장).

**확장 공식 (얼마나 커지나)** — JDK 21 `grow()` 실제 소스:
```java
int newCapacity = ArraysSupport.newLength(oldCapacity,
    minCapacity - oldCapacity, /* minimum growth */
    oldCapacity >> 1           /* preferred growth */);
return elementData = Arrays.copyOf(elementData, newCapacity);
```

- **1.5배 확장** (`oldCapacity >> 1` = oldCapacity / 2 를 더함)
- 예: 10 → 15 → 22 → 33 → 49 → 73 → ...
- `Arrays.copyOf()` 호출 = **새 배열 할당 + 전체 복사** (O(n))

**버전별 확장 공식 변천사** (trigger는 모두 동일, 공식만 미세 변화):
| JDK | 확장 공식 |
|-----|---------|
| 1.2 ~ 6 | `(oldCapacity * 3) / 2 + 1` (약 1.5x + 1) |
| 7 ~ 8 | `oldCapacity + (oldCapacity >> 1)` (정확히 1.5x, +1 제거) |
| 9 ~ 25 | `ArraysSupport.newLength(oldCapacity, minGrowth, oldCapacity >> 1)` (1.5x + 오버플로우 안전 처리) |

**왜 1.5배인가?**
- 2배는 메모리 낭비가 큼 (극단적으로 2배씩 커지면 사용률 50% 미만일 수 있음)
- 1.5배는 메모리/성능 trade-off의 균형점. amortized O(1) 보장은 유지됨.
- 참고: C++ `std::vector`는 보통 2배 (구현마다 다름), Python `list`는 1.125배 근사.

#### 성능 특성

| 연산 | 시간 복잡도 | 비고 |
|------|-----------|------|
| `get(i)` | O(1) | 배열 인덱싱 |
| `add(e)` (끝에 추가) | amortized O(1) | resize 발생 시 그 호출만 O(n) |
| `add(i, e)` (중간 삽입) | O(n) | 이후 원소 shift |
| `remove(i)` | O(n) | 이후 원소 shift |
| `contains(o)` | O(n) | 선형 탐색 |

#### 초기 용량 지정 Tip

```java
// Bad ❌ (10 → 15 → 22 → ... → 1,500까지 가는 동안 매번 복사)
List<Integer> list = new ArrayList<>();
for (int i = 0; i < 1000; i++) list.add(i);

// Good ✅ (resize 0회)
List<Integer> list = new ArrayList<>(1000);
```

---

## HashSet — HashMap을 재활용한 중복 제거

#### 내부 구조 (JDK 21 소스 verified)

```java
public class HashSet<E> extends AbstractSet<E> implements Set<E> {
    transient HashMap<E, Object> map;        // ← 실제 저장소는 HashMap
    static final Object PRESENT = new Object();  // 모든 value로 쓰는 더미 객체
}
```

- `HashSet`은 **얇은 wrapper**. 로직 전부 `HashMap`에 위임.
- 원소를 `HashMap`의 **key**로 저장. value는 전역 공유 더미 `PRESENT`.

#### add / contains / remove

```java
// 실제 JDK 21 소스
public boolean add(E e) {
    return map.put(e, PRESENT) == null;   // 처음 넣는 경우 null 반환 → true
}
public boolean contains(Object o) {
    return map.containsKey(o);
}
public boolean remove(Object o) {
    return map.remove(o) == PRESENT;
}
```

- **왜 더미 value?** `HashMap`의 value 슬롯을 null로 두면 "key 없음"과 구분이 안 됨. 그래서 `PRESENT`를 넣어 "key 존재" 신호로 씀.
- 모든 성능 특성 = `HashMap`과 동일 (평균 O(1)).

---

## HashMap — 해시 테이블의 Java 구현

#### 내부 구조

```java
transient Node<K,V>[] table;    // 버킷 배열 (초기 null, 첫 put 때 할당)
transient int size;             // 전체 엔트리 수
int threshold;                  // 리사이즈 발동 임계값 = capacity * loadFactor
final float loadFactor;         // 기본 0.75

static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;             // ← 체이닝용 연결 리스트 포인터
}
```

**verified 상수 (JDK 21 reflection으로 확인)**:
- `DEFAULT_INITIAL_CAPACITY = 16` (= `1 << 4`)
- `MAXIMUM_CAPACITY = 1073741824` (= `1 << 30`)
- `DEFAULT_LOAD_FACTOR = 0.75`
- `TREEIFY_THRESHOLD = 8`
- `UNTREEIFY_THRESHOLD = 6`
- `MIN_TREEIFY_CAPACITY = 64`

#### `hash()` — 왜 XOR high bits인가

```java
// 실제 JDK 21 소스
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

- `key.hashCode()`의 **상위 16비트를 하위 16비트로 XOR**
- **왜?** 인덱스 계산은 `(n - 1) & hash`인데, `n`이 작으면 하위 몇 비트만 사용 → 상위 비트가 전혀 반영 안 됨. 상위 비트를 XOR로 하위에 섞어서 **낮은 비트의 분포 품질**을 끌어올림.
- 비용은 "shift + XOR" 두 번 뿐 → 성능 거의 공짜.

#### 인덱스 계산

```java
int index = (n - 1) & hash;   // n은 table.length, 반드시 2의 거듭제곱
```

- `n`이 2의 거듭제곱이라 `n - 1`은 하위 비트가 전부 1 (e.g., 16 → 15 = `0b1111`)
- `& hash` = **하위 log₂(n) 비트** 추출 (= `hash % n`과 동치, 하지만 더 빠름)

#### 해시 충돌 처리 — JDK 7 vs JDK 8+ (⭐ 핵심 차이)

**JDK 7 이하**: **separate chaining with linked list**
- 같은 버킷 → 연결 리스트로 연결
- 최악 O(n) — 모든 키가 같은 버킷으로 가면 선형 탐색

**JDK 8+**: **chaining + 트리화 (treeify)**
- 리스트 길이가 **8 (`TREEIFY_THRESHOLD`)** 이상이고 테이블 크기가 **64 (`MIN_TREEIFY_CAPACITY`)** 이상이면 → **Red-Black Tree로 변환**
- 트리에서는 조회 O(log n). n=1000 collisions라도 트리 조회 10회 수준.
- 리스트 길이가 **6 (`UNTREEIFY_THRESHOLD`)** 이하로 줄면 다시 리스트로 복귀 (히스테리시스로 oscillation 방지).

**왜 치환 임계값이 서로 다른가?** (8 vs 6)
- 8↔8이면 트리/리스트 경계에서 왔다갔다 할 때마다 변환 비용 발생
- 2칸의 간격을 둬서 안정화

**왜 꼭 테이블 size 64도 필요한가?**
- 테이블이 작을 땐 트리화보다 resize가 더 저렴. 작은 테이블의 충돌은 보통 "이제 resize할 때" 신호임.

#### `resize()` 메커니즘

```java
// JDK 21 resize() 요약
final Node<K,V>[] resize() {
    int newCap = oldCap << 1;              // ← 2배로 확장
    int newThr = oldThr << 1;              // threshold도 2배
    Node<K,V>[] newTab = new Node[newCap];
    // 각 노드를 새 버킷으로 재분배
    // JDK 8+는 (hash & oldCap)으로 "low/high" 두 그룹으로 분할 (전체 rehash 불필요)
}
```

**발동 조건**: `size > threshold` (threshold = capacity × loadFactor)
- 예: capacity=16, loadFactor=0.75 → threshold=12 → **13번째 put에서 resize 발동**
- 리사이즈 후 capacity=32, threshold=24

**JDK 8+의 영리한 재배치**:
- 2배 확장이라 각 노드의 **새 인덱스는 둘 중 하나**: `원래 인덱스` or `원래 인덱스 + oldCap`
- `(hash & oldCap) == 0`이면 그대로, 아니면 `+ oldCap` 위치로
- 전체 재해싱 없이 **단일 비트 체크로 분기** → O(n)이지만 상수항이 작음

#### 성능 특성

| 연산 | 평균 | 최악 (JDK 7) | 최악 (JDK 8+) |
|------|------|-------------|---------------|
| `get(k)` | O(1) | O(n) | O(log n) |
| `put(k,v)` | O(1) | O(n) | O(log n) |
| `remove(k)` | O(1) | O(n) | O(log n) |

JDK 8의 treeify는 **해시 플러드 공격(hash flooding) 완화**에도 중요 — 악의적으로 모든 키의 hashCode를 동일하게 만든 입력이 들어와도 O(log n)으로 방어.

---

## JDK 버전별 변화 요약

| JDK | 주요 변화 |
|-----|---------|
| 1.2 | `HashMap`, `HashSet`, `ArrayList` 포함 Collections Framework 도입 |
| 5 | 제네릭 적용. `StringBuilder` 등 |
| 7 | `ArrayList.grow()` 공식 변경 (`newCapacity = oldCapacity + (oldCapacity >> 1)` 명시화) |
| **8** | **`HashMap` treeify 도입** (가장 큰 변화). `TREEIFY_THRESHOLD = 8`, red-black tree. resize 알고리즘도 2배 확장 활용해서 개선 |
| 9 | Compact Strings (JEP 254) — 간접 영향: 문자열 key의 해시 계산 더 빠름 |
| 11 | 특별한 변화 없음 |
| 17 | 특별한 변화 없음 |
| 21 | 특별한 변화 없음 |
| 25 | 특별한 변화 없음 |

→ 사실상 **JDK 8 이후로 Collection 내부 알고리즘은 거의 안정**. JDK 8의 treeify가 가장 큰 분기점.

---

## 주의점 / 함정

- **`hashCode()`와 `equals()`는 항상 같이 override**. 하나만 override하면 `HashMap`/`HashSet`에서 조회 안 됨. (IDE 자동 생성 사용 추천.)
- **mutable 객체를 key로 쓰면 지옥**:
  ```java
  Map<Point, String> m = new HashMap<>();
  Point p = new Point(1, 1);
  m.put(p, "origin");
  p.setX(999);              // key의 hashCode 바뀜
  m.get(p);                 // null! 해시가 바뀌어서 못 찾음
  ```
- **`HashMap`은 스레드 안전 X**. 멀티 스레드에서 put/resize 중 **무한 루프** 터지는 유명 버그 (JDK 7). JDK 8에서는 데이터 유실로 완화됐지만 여전히 안전하지 않음. 멀티 스레드엔 `ConcurrentHashMap`.
- **초기 capacity 지정**:
  ```java
  // 1000개 넣을 거 안다면
  Map<K,V> m = new HashMap<>(1000 / 0.75f + 1);  // threshold 넘지 않게
  // resize 0회
  ```
- **`ArrayList.contains()`는 O(n)**. 멤버십 체크가 잦으면 `HashSet`으로.
- **`LinkedHashMap`은 순서 보장이 필요할 때만**. 삽입 순서(기본) or LRU(access-order) 모드. 일반 `HashMap`보다 약간 느림.

---

## 참고 자료

- OpenJDK 21 소스: `$JAVA_HOME/lib/src.zip` → `java.base/java/util/{ArrayList,HashMap,HashSet}.java`
- JEP 180: Handle Frequent HashMap Collisions with Balanced Trees (JDK 8)
- JEP 254: Compact Strings (JDK 9)
