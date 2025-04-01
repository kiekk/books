# 📘 [연관관계] - @ManyToMany에서 Set이 List보다 나은 이유

---

## 📖 전반적인 내용

이 장에서는 **[@ManyToMany에서 Set이 List보다 나은 이유]** 에 대해 학습합니다.

- List 컬렉션을 사용하면 매핑 테이블에 있는 모든 데이터를 삭제 후 다시 등록하는 쿼리가 실행되는 반면
Set 컬렉션을 사용하면 매핑 테이블에 해당 데이터만 삭제하는 쿼리가 실행됩니다.
- Set 컬렉션은 순서가 없다는 단점이 있는데 @OrderBy를 사용하여 순서가 보장된 Set 컬렉션을 사용할 수 있습니다.

---

## 🔍 중심 로직 캡처

다음은 이 장에서 핵심적으로 다룬 코드입니다:

```java
@Entity
public class AuthorList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "author_book_list",
            joinColumns = @JoinColumn(name = "author_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private List<BookList> books = new ArrayList<>();
    
    ...
}
```

```java
public class BookList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToMany(mappedBy = "books")
    private List<AuthorList> authors = new ArrayList<>();
    
    ...
}
```

```java
@Entity
public class AuthorSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "author_book_set",
            joinColumns = @JoinColumn(name = "author_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<BookSet> books = new HashSet<>();
    
    ...
}
```

```java
@Entity
public class BookSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToMany(mappedBy = "books")
    @OrderBy("name ASC") // HashSet을 사용하더라도 내부적으로는 LinkedHashSet을 사용하기 때문에 LinkedHashSet을 할당하는 것이 더 좋습니다.
    private Set<AuthorSet> authors = new LinkedHashSet<>();
    
    ...
}
```

---

## 🧪 쿼리 비교

- `@ManyToMany`에서 List를 사용한 경우
```sql
================================================
Removing a book (List case) ...
================================================

# author_book_list 매핑 테이블 데이터 삭제
Hibernate:
delete
from
    author_book_list
where
    author_id=?
    
# author_book_list 매핑 테이블 데이터 재등록
Hibernate:
insert
into
    author_book_list
    (author_id, book_id)
values
    (?, ?)
Hibernate:
insert
into
    author_book_list
    (author_id, book_id)
values
    (?, ?)
```

- `@ManyToMany`에서 Set을 사용한 경우
```sql
================================================
Removing a book (Set case) ...
================================================
# author_book_list 매핑 테이블에 해당 데이터만 삭제
Hibernate: 
    delete 
    from
        author_book_set 
    where
        author_id=? 
        and book_id=?
```

- `@ManyToMany`에서 Set + @OrderBy를 사용한 경우
```sql
Hibernate: 
    select
        a1_0.book_id,
        a1_1.id,
        a1_1.age,
        a1_1.genre,
        a1_1.name 
    from
        author_book_set a1_0 
    join
        author_set a1_1 
            on a1_1.id=a1_0.author_id 
    where
        a1_0.book_id=? 
    order by
        a1_1.name
```

📌
- `@ManyToMany`에서 컬렉션을 List로 사용하면 도서 추가/삭제 시 마다 매번 author_book_list 매핑 테이블의 데이터를 모두 제거 후 다시 등록하는 쿼리가 실행됩니다.
- 반면 Set을 사용하면 매핑 테이블에 해당 데이터만 삭제하는 쿼리가 실행됩니다.
- Set 컬렉션은 순서가 없다는 단점이 있는데 @OrderBy를 사용하여 순서가 보장된 Set 컬렉션을 사용할 수 있습니다.
  (주의: 이 때 구현체를 HashSet으로 사용해도 내부적으로는 LinkedHashSet을 사용하기 때문에 구현체도 LinkedHashSet으로 맞춰서 사용합니다.)
  - @OrderBy를 사용하면 실제 쿼리에 order by 구문이 추가되어 실행됩니다.

---

## 💬 나의 코멘트
- 실무에서는 @ManyToMany를 사용하기 보다는 @OneToMany와 @ManyToOne으로 많이 사용하기 때문에 이번 챕터를
@ManyToMany를 사용하지 말아야 할 이유로 받아들였습니다.
- @ManyToMany에서 List 컬렉션을 사용할 경우 이슈가 있다고 하는데 이 부분도 참고해보면 좋을 것 같습니다.

---

## 📂 참고할만한 자료

- Hibernate HHH-5855 이슈
    - [link](https://hibernate.atlassian.net/browse/HHH-5855)