# 📘 [연관관계] - JPA 엔터티 서브그래프를 통해 연관관계를 가져오는 방법

---

## 📖 전반적인 내용

이 장에서는 **[JPA 엔터티 서브그래프를 통해 연관관계를 가져오는 방법]** 에 대해 학습합니다.

- 엔터티 서브그래프는 `@NamedSubgraph` 또는 애드혹 엔터티 서브그래프를 사용하여 정의할 수 있습니다.
    - `@NamedSubgraph`: 엔터티 클래스에 선언, `@NamedEntityGraph`에 선언한 subgraphs 속성에 `@NamedSubgraph`의 name 속성을 설정
    - 애드혹 엔터티 그래프: `@EntityGraph`를 사용하여 동적으로 생성, 엔터티 클래스에 선언한 연관관계 필드명을 JPA 메서드에 attributePaths 속성에 .(dot) 노테이션으로 설정

---

## 🔍 중심 로직 캡처

다음은 이 장에서 핵심적으로 다룬 코드입니다:

- `@NamedSubgraph`

```java
@NamedEntityGraph(
        name = "publisher-books-graph",
        attributeNodes = {
                @NamedAttributeNode(value = "books", subgraph = "author-subgraph")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "author-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("author")
                        }
                )
        }
)
public class Publisher {
    @OneToMany(cascade = CascadeType.ALL,
            mappedBy = "publisher", orphanRemoval = true)
    private List<Book> books = new ArrayList<>();
}

public interface PublisherRepository extends JpaRepository<Publisher, Long>, JpaSpecificationExecutor<Publisher> {
    @Override
    @EntityGraph(value = "publisher-books-graph", type = EntityGraph.EntityGraphType.FETCH)
    List<Publisher> findAll();
}
```

- `애드혹 엔터티 서브그래프`

```java
public interface PublisherRepository extends JpaRepository<Publisher, Long>, JpaSpecificationExecutor<Publisher> {
    @Override
    @EntityGraph(attributePaths = {"books.author"}, type = EntityGraph.EntityGraphType.FETCH)
    List<Publisher> findAll();
}
```

---

