# 📘 [연관관계] - @ManyToMany 연관관계를 효과적으로 구성하는 방법

---

## 📖 전반적인 내용

이 장에서는 **[@ManyToMany 연관관계를 효과적으로 구성하는 방법]** 에 대해 학습합니다.

- `@ManyToMany` 매핑을 사용하려면 연관관계의 주인을 설정해야 한다.
- `@ManyToMany` 매핑 시 컬렉션은 List 대신 Set을 사용하는 것이 성능이 훨씬 좋다. (항목 5에서 자세하게 설명)
- 대부분의 경우에는 `CascadeType.ALL` 및 `CascadeType.REMOVE`를 사용하지 않는 것이 좋다.
- `orphanRemoval` 속성은 `@ManyToOne`, `@ManyToMany` 에는 지정되지 말아야 한다.
- `@ManyToMany` 연관관계는 `@OneToMany`와 `@ManyToOne`으로 풀어내는 것이 좋다.
  (DB 설계와 동일하게 다대다는 다대일과 일대다로 풀어내는 것이 좋다.)

---

## 🔍 중심 로직 캡처

다음은 이 장에서 핵심적으로 다룬 코드입니다:

```java
@Entity
@Table(name = "author")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "author_book",
            joinColumns = @JoinColumn(name = "author_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<Book> books = new HashSet<>();
    
  ...
}
```

```java
@Entity
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToMany(mappedBy = "books")
    private Set<Author> authors = new HashSet<>();
  
  ...
}
```

📌   
- `@ManyToMany`에서는 `CascadeType.ALL` 과 `CascadeType.REMOVE`를 사용하지 않는 것이 좋다.

---

## 🧪 쿼리 비교

```sql
# author 등록
Hibernate: 
    insert 
    into
        author
        (age, genre, name, id) 
    values
        (?, ?, ?, default)

# author에 연결된 book 등록
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
  
# author 등록
Hibernate: 
    insert 
    into
        author
        (age, genre, name, id) 
    values
        (?, ?, ?, default)
  
# author에 연결된 book 등록
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
         
# author_book 등록
Hibernate: 
    insert 
    into
        author_book
        (author_id, book_id) 
    values
        (?, ?)
Hibernate: 
    insert 
    into
        author_book
        (author_id, book_id) 
    values
        (?, ?)
```

---

## 💬 나의 코멘트
- `@ManyToMany`를 사용할 경우 위와 같은 점들을 주의하며 사용해야 하지만, 그보다 더 앞서 `@ManyToMany` 연관관계를 꼭 사용해야 하는가?에 대해 고민해볼 필요가 있다.
- 실제로 `@ManyToMany` 연관관계는 `@OneToMany`와 `@ManyToOne`으로 풀어내는 것이 좋다.
- 책에서는 List보다 Set을 사용하라고 하지만 지금 예제에서는 Set, List가 동일하게 동작하기 때문에 항목 5에서 이 부분에 대해 확인해보겠습니다.

---
