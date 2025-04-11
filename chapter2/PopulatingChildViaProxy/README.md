# 📘 [엔터티] - 하이버네이트 프록시를 통한 자식 측에서 부모 연관관계 채우기

---

## 📖 전반적인 내용

이 장에서는 **[하이버네이트 프록시를 통한 자식 측에서 부모 연관관계 채우기]** 에 대해 학습합니다.

- 연관관계 설정시 외래키만 필요할 경우 엔터티를 전부 가져오는 것은 성능 저하를 가질 수 있습니다.
- 하이버네이트는 초기화되지 않은 프록시에 대한 외래키 값을 설정할 수 있기 때문에 외래키만 필요할 경우에는 프록시를 사용하여 연관관계를 설정할 수 있습니다.
- 프록시 객체를 조회하기 위해서는 `EntityManager#getReferenceById()` 메서드를 사용합니다.
- 예제에 나오는 `EntityManager#getOne()`은 `deprecated` 되었기 때문에 대신 `EntityManager#getReferenceById()`를 사용합니다.

---

## 🔍 중심 로직 캡처

- `findById()` 메서드 동작 확인
```java
@Service
public class BookstoreService {
    @Transactional
    public void addBookToAuthorWithFindById() {
        Author author = authorRepository.findById(1L).orElseThrow();

        Book book = Book.createBook("The Canterbury Anthology", "001-MJ", author);

        bookRepository.save(book);
    }
}
```

```sql
Call addBookToAuthorWithFindById();
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

Hibernate: 
    insert 
    into
        book
        (author_id, isbn, title, id) 
    values
        (?, ?, ?, default)
```

- `getReferenceById()` 메서드 동작 확인
```java
@Service
public class BookstoreService {
    @Transactional
    public void addBookToAuthor() {
        Author proxy = authorRepository.getReferenceById(1L);

        Book book = Book.createBook("The Canterbury Anthology", "001-MJ", proxy);

        bookRepository.save(book);
    }
}
```

```sql
Call addBookToAuthor();
Hibernate: 
    insert 
    into
        book
        (author_id, isbn, title, id) 
    values
        (?, ?, ?, default)
```

📌
- `findById()` 는 Author 엔티티를 조회하기 위해 `select` 쿼리를 실행합니다.
- 반면 `getReferenceById()` 는 Author 엔티티를 조회하기 위해 `select` 쿼리를 실행하지 않습니다.

---

## 💬 나의 코멘트
- 연관관계 설정만 할 경우에는 외래키만 설정하면 되기 떄문에 `getReferenceById()` 메서드를 사용하여 성능을 개선할 수 있습니다.
- 만약 `Author` 객체가 이미 영속성 컨텍스트에 있다면 크게 상관은 없겠지만 예제와 같이 `Author`를 다시 조회해야 하는 경우에는 위와 같은 방법으로 프록시 객체를 조회하는 것이 더 좋습니다.

---
