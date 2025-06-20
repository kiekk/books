# 📘 [연관관계] - CascadeType.REMOVE 및 orphanRemoval=true를 사용해 하위 엔터티 제거를 피해야 하는 이유와 시기

---

## 📖 전반적인 내용

이 장에서는 **[CascadeType.REMOVE 및 orphanRemoval=true를 사용해 하위 엔터티 제거를 피해야 하는 이유와 시기]** 에 대해 학습합니다.



---

## 🔍 중심 로직 캡처

다음은 이 장에서 핵심적으로 다룬 코드입니다:

```java
// Author.java
@OneToMany(cascade = CascadeType.ALL, mappedBy = "author", orphanRemoval = true)
private List<Book> books = new ArrayList<>();

// Book.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "author_id")
private Author author;
```

📌
- `orphanRemoval = true`를 사용하면 부모 엔터티가 삭제될 때 자식 엔터티도 함께 삭제됩니다.
  - 여기서의 삭제는 컬렉션에서의 삭제를 의미한다.
- `CascadeType.REMOVE`는 부모 엔터티가 삭제될 때 자식 엔터티도 함께 삭제되도록 설정합니다.
  - 여기서의 삭제는 DB에서의 삭제를 의미한다.

```java
// Author.java
public void removeBook(Book book) {
    book.removeAuthor();
    this.books.remove(book);
}
```

📌
- orphanRemoval=false일 경우 removeBook() 메서드를 호출하면 `UPDATE` 쿼리가 발생합니다.
- orphanRemoval=true일 경우 removeBook() 메서드를 호출하면 `DELETE` 쿼리가 발생합니다.

---

## 🧪 쿼리 비교

## deleteViaCascadeRemove() 메서드 동작 확인
- Author 조회 후 Author 삭제
- Author 만 조회하기 때문에 books 는 프록시 객체

```java
@Transactional
public void deleteViaCascadeRemove() {
  Author author = authorRepository.findByName("Joana Nimar");

  authorRepository.delete(author);
}
```

```sql
# author 조회
Hibernate: 
    select
        a1_0.id,
        a1_0.age,
        a1_0.genre,
        a1_0.name 
    from
        author a1_0 
    where
        a1_0.name=?
    
# books 조회
Hibernate: 
    select
        b1_0.author_id,
        b1_0.id,
        b1_0.isbn,
        b1_0.title 
    from
        book b1_0 
    where
        b1_0.author_id=?
      
# book 제거
Hibernate: 
    delete 
    from
        book 
    where
        id=?
Hibernate: 
    delete 
    from
        book 
    where
        id=?
Hibernate: 
    delete 
    from
        book 
    where
        id=?
      
# author 제거
Hibernate: 
    delete 
    from
        author 
    where
        id=?
```

📌 
- 먼저 Author를 조회하는데 이 때는 books를 같이 조회하지 않기 때문에 books는 프록시 객체입니다.
- Author 제거 전 books를 제거하려고 하는데 books가 영속성 컨텍스트에 없기 때문에 books를 조회합니다.
- 조회한 books를 제거 후 마지막에 author를 제거합니다.

## deleteViaOrphanRemoval() 메서드 동작 확인
- Author 조회 후 Author 삭제
- Author 조회 시 fetch join을 사용하여 books를 같이 조회

```java
@Transactional
public void deleteViaOrphanRemoval() {
    Author author = authorRepository.findByNameWithBooks("Joana Nimar");

    author.removeBooks();
    authorRepository.delete(author);
}
```

```sql
# author, books 조회
Hibernate: 
    select
        a1_0.id,
        a1_0.age,
        b1_0.author_id,
        b1_0.id,
        b1_0.isbn,
        b1_0.title,
        a1_0.genre,
        a1_0.name 
    from
        author a1_0 
    join
        book b1_0 
            on a1_0.id=b1_0.author_id 
    where
        a1_0.name=?
      
# books 제거
Hibernate: 
    delete 
    from
        book 
    where
        id=?
Hibernate: 
    delete 
    from
        book 
    where
        id=?
Hibernate: 
    delete 
    from
        book 
    where
        id=?
    
# author 제거
Hibernate: 
    delete 
    from
        author 
    where
        id=?
```

📌
- 먼저 Author를 조회하는데 fetch join을 사용하여 books를 같이 조회하기 때문에 books는 영속성 컨텍스트에 있습니다.
- Author 제거 전 books를 제거하려고 하는데 books가 영속성 컨텍스트에 있기 때문에 books를 DB에서 조회하지 않고 바로 제거합니다.
- books를 제거 후 마지막에 author를 제거합니다.

## deleteViaIdentifiers() 메서드 동작 확인
- Author 조회 후 Author 삭제
- bookRepository를 사용하여 authorId로 books 제거

**deleteViaIdentifiersX() 는 fetch join으로 books도 조회하긴 하지만 동작 방식은 동일합니다.**

```java
@Transactional
public void deleteViaIdentifiers() {
    Author author = authorRepository.findByName("Joana Nimar");

    bookRepository.deleteByAuthorIdentifier(author.getId());
    authorRepository.deleteByIdentifier(author.getId());
}
```

```sql
# author 조회
Hibernate: 
    select
        a1_0.id,
        a1_0.age,
        a1_0.genre,
        a1_0.name 
    from
        author a1_0 
    where
        a1_0.name=?
      
# authorId로 books 제거
Hibernate: 
    delete 
    from
        book b1_0 
    where
        b1_0.author_id=?

# author 제거
Hibernate: 
    delete 
    from
        author a1_0 
    where
        a1_0.id=?
```

📌
- 이번에는 books 제거를 bookRepository를 사용하여 authorId로 제거하기 때문에 books를 조회하지 않습니다.


## deleteViaBulkIn() 메서드 동작 확인
- 여러 개의 Author를 삭제해야 할 경우 bulk 연산을 사용하기

```java
@Transactional
public void deleteViaBulkIn() {
    List<Author> authors = authorRepository.findByAge(34);

    bookRepository.deleteBulkByAuthors(authors);
    authorRepository.deleteInBatch(authors);
}
```

```sql
# author 조회
Hibernate: 
    select
        a1_0.id,
        a1_0.age,
        a1_0.genre,
        a1_0.name 
    from
        author a1_0 
    where
        a1_0.age=?
      
# book을 in절로 제거
Hibernate: 
    delete 
    from
        book b1_0 
    where
        b1_0.author_id in (?, ?)

# author을 or절로 제거
Hibernate: 
    delete 
    from
        author a1_0 
    where
        a1_0.id=? 
        or a1_0.id=?
```

📌
- author 제거 시 books를 추가로 조회하지 않고 in절로 제거합니다.
- Author, Book 제거 시 Author/Book 수의 상관 없이 DELETE 쿼리가 2개만 발생합니다.

## deleteViaDeleteInBatch() 메서드 동작 확인
- 여러 개의 Author 삭제 시 bulk 연산 사용 (fetch join으로 books 같이 조회)

```java
@Transactional
public void deleteViaDeleteInBatch() {
    Author author = authorRepository.findByNameWithBooks("Joana Nimar");

    bookRepository.deleteInBatch(author.getBooks());
    authorRepository.deleteInBatch(List.of(author));
}
```

```sql
# author, books 조회
Hibernate: 
    select
        a1_0.id,
        a1_0.age,
        b1_0.author_id,
        b1_0.id,
        b1_0.isbn,
        b1_0.title,
        a1_0.genre,
        a1_0.name 
    from
        author a1_0 
    join
        book b1_0 
            on a1_0.id=b1_0.author_id 
    where
        a1_0.name=?
      
# books 제거 (or절로 제거)
Hibernate: 
    delete 
    from
        book b1_0 
    where
        b1_0.id=? 
        or b1_0.id=? 
        or b1_0.id=?
      
# author 제거
Hibernate: 
    delete 
    from
        author a1_0 
    where
        a1_0.id=?
```

📌
- author 제거 시 books를 추가로 조회하지 않고 or절로 제거합니다.

## deleteViaHardCodedIdentifiers() 메서드 동작 확인
- author, book 모두 영속성 컨텍스트에 없는 상태에서 삭제

```java
@Transactional
public void deleteViaHardCodedIdentifiers() {
    bookRepository.deleteByAuthorIdentifier(4L);
    authorRepository.deleteByIdentifier(4L);
}
```

```sql
# book 제거
Hibernate: 
    delete 
    from
        book b1_0 
    where
        b1_0.author_id=?

# author 제거
Hibernate: 
    delete 
    from
        author a1_0 
    where
        a1_0.id=?
```

📌
- author, book을 조회하는 쿼리 없이 바로 식별자로 제거

## deleteViaHardCodedIdentifiersX() 메서드 동작 확인
- deleteViaHardCodedIdentifiers() 방식에서 bulk로 연산으로 변경 (IN절 사용) 

```java
@Transactional
public void deleteViaBulkHardCodedIdentifiers() {
    List<Long> authorsIds = Arrays.asList(1L, 4L);

    bookRepository.deleteBulkByAuthorIdentifier(authorsIds);
    authorRepository.deleteBulkByIdentifier(authorsIds);
}
```

```sql
# book 제거
Hibernate: 
    delete 
    from
        book b1_0 
    where
        b1_0.author_id in (?, ?)
      
# author 제거
Hibernate: 
    delete 
    from
        author a1_0 
    where
        a1_0.id in (?, ?)
```

---

## 💬 나의 코멘트
- deleteViaIdentifiers() 방식은 직접 개발자가 식별자를 지정하기 때문에 절차형 프로그래밍 (트랜잭션 스크립트) 방식처럼 보인다.
- 반면 deleteViaCascadeRemove(), deleteViaOrphanRemoval()는 JPA의 기능을 활용하며 컬렉션을 이용해 관리하기 때문에 
  객체지향 프로그래밍 (도메인 모델) 방식처럼 보인다.

---

## 📂 참고할만한 자료
