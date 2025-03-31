# 📘 [연관관계] - 단방향 @ManyToOne의 효율성

---

## 📖 전반적인 내용

이 장에서는 **[단방향 @ManyToOne의 효율성]** 에 대해 학습합니다.

- 단방향 연관관계는 일대다 단방향 관관계(@OneToMany) 보다 다대일 단방향 연관관계(@ManyToOne)이 더 효율적이다.
- 순위로 치자면 단방향 연관관계가 가능할 때
  - 1.@ManyToOne 단방향 연관관계
  - 2.@OneToMany, @ManyToOne 양방향 연관관계
  - 3.@OneToMany 단방향 연관관계
  - 순으로 설정하는 것이 좋다.

---

## 🔍 중심 로직 캡처

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

    @ManyToOne(fetch = FetchType.LAZY) // 지연로딩으로 설정
    @JoinColumn(name = "author_id")
    private Author author;
    
  ...
}
```

📌
- 다대일 단방향 연관관계에서는 Author 엔티티에 Book 엔티티만 설정해주면 됩니다.

---

## 🧪 쿼리 비교

```sql
Insert new book to an author ...
---------------------------------------------
Hibernate: 
    insert 
    into
        book
        (author_id, isbn, title, id) 
    values
        (?, ?, ?, default)
```

```sql
Fetch a list of books and delete the first book ...
---------------------------------------------
Hibernate: 
    select
        b1_0.id,
        b1_0.author_id,
        b1_0.isbn,
        b1_0.title 
    from
        book b1_0 
    where
        b1_0.author_id=?
Hibernate: 
    select
        a1_0.id,
        a1_0.age,
        a1_0.genre,
        a1_0.name 
    from
        author a1_0 
    where
        a1_0.id=?
[Book{id=2, title='A Peoples History', isbn='002-JN', author=Author{id=4, name='Joana Nimar', genre='History', age=34}}, Book{id=5, title='History Of Present', isbn='not available', author=Author{id=4, name='Joana Nimar', genre='History', age=34}}, Book{id=6, title='History Facts', isbn='004-JN', author=Author{id=4, name='Joana Nimar', genre='History', age=34}}]
Hibernate: 
    delete 
    from
        book 
    where
        id=?
```

📌
- 쿼리도 외래키 관련 추가 update 쿼리 없이 간결하게 실행되는 것을 확인할 수 있습니다.

---

## 💬 나의 코멘트

- 1 <-> N 연관관계를 설정할 경우 특별한 이유가 없는 한 양방향 연관관계로 설정하는 것이 좋다.
- 만약 단방향 연관관계로 설정하고 싶다면 다대일 단방향 연관관계 (@ManyToOne)로 설정하는 것이 좋다.
- 일대다 단방향 연관과녜는 @OneToMany 저번에도 살펴봤듯이 불필요한 쿼리들이 추가로 발생하기 때문에 되도록 사용하지 않는 것이 좋다.  
---
