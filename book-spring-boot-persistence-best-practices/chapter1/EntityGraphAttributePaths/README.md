# 📘 [연관관계] - JPA 엔터티 그래프를 통해 연관관계를 가져오는 방법

---

## 📖 전반적인 내용

이 장에서는 **[JPA 엔터티 그래프를 통해 연관관계를 가져오는 방법]** 에 대해 학습합니다.

- `EntityGraph.EntityGraphType.FETCH`
  - attributeNodes에 지정되는 속성들은 `FetchType.EAGER`로 처리되며 나머지 속성들은 기본/명시적 설정에 상관없이 `FetchType.LAZY`로 처리됩니다.
- `EntityGraph.EntityGraphType.LOAD`
  - attributeNodes에 지정되는 속성들은 `FetchType.EAGER`로 처리되며 나머지 속성들은 지정된 `FetchType`이나 기본 `FetchType`에 따라 처리됩니다.

- 엔터티 그래프는 `@NamedEntityGraph` 또는 애드혹 엔터티 그래프를 사용하여 정의할 수 있습니다.
  - `@NamedEntityGraph`: 엔터티 클래스에 선언, `@NamedEntityGraph`에 선언한 name 속성을 JPA 메서드에 `@EntityGraph`에 value 속성에 설정
  - 애드혹 엔터티 그래프: `@EntityGraph`를 사용하여 동적으로 생성, 엔터티 클래스에 선언한 연관관계 필드명을 JPA 메서드에 attributePaths 속성에 설정


---

## 🔍 중심 로직 캡처

다음은 이 장에서 핵심적으로 다룬 코드입니다:

- `@NamedEntityGraph`

```java
// Author.java
@NamedEntityGraph(
        name = "author-books-graph",
        attributeNodes = {
                @NamedAttributeNode("books")
        }
)
@Entity
public class Author {
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "author", orphanRemoval = true)
  private List<Book> books = new ArrayList<>();
}

// Book.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id")
private Author author;

// AuthorRepository.java
@EntityGraph(value = "author-books-graph", type = EntityGraph.EntityGraphType.FETCH)
@Override
List<Author> findAll();
```

- `애드혹 엔터티 그래프`

```java
// Author.java
@Entity
public class Author {
  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "author", orphanRemoval = true)
  private List<Book> books = new ArrayList<>();
}

// Book.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id")
private Author author;

// AuthorRepository.java
@EntityGraph(attributePaths = {"books"}, type = EntityGraph.EntityGraphType.FETCH)
@Override
List<Author> findAll();
```

📌
- `@NamedEntityGraph`를 사용하면 엔터티 클래스에서 연관관계 그래프를 한 번에 확인할 수 있다는 것이 장점입니다.
- 애드혹 엔터티 그래프는 메서드 단위로 연관관계 그래프를 설정할 수 있다는 것이 장점입니다.
  - 엔터티 그래프 정의를 Repository 수준에서 유지하고 `@NamedEntityGraph`로 엔터티를 변경하지 않는 편리한 방법입니다.

---

## 💬 나의 코멘트
- 개인적으로는 책에서 설명했던 내용대로 Repository 수준에서 엔터티 그래프를 한 번에 관리할 수 있어서 `@NamedEntityGraph` 보다는 애드혹 엔터티 그래프를 사용하는 것이 더 편리했었습니다.
- 책에서는 `EntityManager`를 사용하여 직접 엔터티 그래프를 정의하는 방법도 소개하고 있지만 이 방법은 실제 사용하지 않는 방법이라고 판단하여 이 부분은 눈으로만 살펴봐도 괜찮을 것 같습니다.

---

## 주의
- 엔터티 그래프와 함께 네이티브 쿼리를 사용하면 하이버네이트 예외가 발생합니다
  - `A native SQL query cannot use EntityGraphs`
- 엔터티 그래프가 연관된 컬렉션을 가져오는 SQL JOIN으로 변환될 때에 페이지네이션 사용에 주의해야 합니다.
  - 메모리 내에서 페이징이 처리되면서 성능 저하가 발생할 수 있습니다.
- 엔터티 그래프를 사용하여 연관관계 즉시 가져오기 (EAGER)를 사용할 때 다수의 컬렉션이 있을 경우 주의해야 합니다.
  - @OneToMany 연관관계가 2개 이상 있을 경우 SELECT가 호출되면 하나 이상의 하이버네이트 `Bag`을 즉시 가져오게 되는데
`MultipleBagFetchException`이 발생하게 됩니다. **카타시안 곱 문제가 발생하기 때문입니다.**
  - ex) A와 연관관계 B, C를 가져올 때 10개의 B행과 25개의 C행을 함께 갖는 25개의 A행이 있다면 최종 결과를 가져오는 카타시안 곱은 25(A) * 10(B) * 20(C) = 5,000행이 됩니다.
  - 최상의 해결책은 한 번에 하나의 연관관계를 가져오는 것입니다.

## 📂 참고할만한 자료

- 카타시안 곱 관련 해결책
  - [link](https://vladmihalcea.com/hibernate-multiplebagfetchexception/)
