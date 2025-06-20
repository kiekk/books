# 📘 [연관관계] - 단방향 @OneToMany 연관관계를 피해야 하는 이유

---

## 📖 전반적인 내용

이 장에서는 **[단방향 @OneToMany 연관관계를 피해야 하는 이유]** 에 대해 학습합니다.

- 1 <-> N 연관관계 설정 시 단방향 연관관계로 설정할 경우 양방향 연관관계와 달리 foreign key 매핑 쿼리가 추가적으로 발생합니다.
- 도서를 추가/삭제 할 경우에도 foreign key 매핑 테이블에 데이터를 전부 삭제한 후 다시 foreign key 매핑 쿼리가 실행됩니다.
- `@JoinColumn`을 사용하면 더 이상 author_books 매핑 테이블이 사용되지 않고 book 엔티티에 foreign key를 저장하는 것을 확인할 수 있습니다.
- `@OrderColumn`을 사용하면 단방향 연관관계가 정렬되는데, 여기서도 Collection 타입 (List/Set)에 따라 다르게 동작합니다.
---

## 🔍 중심 로직 캡처

다음은 이 장에서 핵심적으로 다룬 코드입니다:

```java
@Service
public class BookstoreService {
    ...

    public void insertAuthorWithBooks() {
        Author author = Author.createAuthor("Joana Nimar", "History", 34);
        Book book01 = Book.createBook("001-JN", "A History of Ancient Prague");
        Book book02 = Book.createBook("002-JN", "A People's History");
        Book book03 = Book.createBook("003-JN", "World History");

        author.addBooks(book01, book02, book03); // use addBooks() helper
        authorRepository.save(author);
    }
}

@Entity
@Table(name = "author")
public class Author {
    ...

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Book> books = new ArrayList<>();
    
    public void addBooks(Book... books) {
        for (Book book : books) {
            this.books.add(book);
        }
    }
    ...
}

```

📌   
단방향 연관관계이기 때문에 `mappedBy` 속성을 사용할 필요가 없습니다.
또한 addBooks() 메서드에서도 book 엔티티에 author 엔티티를 설정할 필요가 없습니다.
하지만 단방향의 경우 foreign key 매핑 쿼리가 추가적으로 발생하고 도서를 추가/삭제 할 경우 foreign key 매핑 테이블에 데이터를 전부 삭제한 후 다시 foreign key 매핑 쿼리가 실행됩니다.

---

## 🧪 쿼리 비교

```sql

Insert one author with three books  ...
# author 저장
Hibernate: 
    insert 
    into
        author
        (age, genre, name, id) 
    values
        (?, ?, ?, default)

# book 저장
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
    
# author_books 저장 (book 엔티티에 author_id 설정)
Hibernate: 
    insert 
    into
        author_books
        (author_id, books_id) 
    values
        (?, ?)
Hibernate: 
    insert 
    into
        author_books
        (author_id, books_id) 
    values
        (?, ?)
Hibernate: 
    insert 
    into
        author_books
        (author_id, books_id) 
    values
        (?, ?)

```

📌 **비교 포인트:**
- 만약 author_books 처럼 foreign key 매핑 테이블을 사용하고 싶지 않다면 `@JoinColumn`을 사용한다.
- 앞서 양방향 연관관계에서 `@JoinColumn`을 사용했기 때문에 author_books 매핑 테이블을 사용하지 않았지만, 양방향 연관관계에서도 `@JoinColumn`을 사용하지 않으면 author_books 매핑 테이블이 사용된다.

## @JoinColumn 사용

```java

@Entity
@Table(name = "author")
public class Author {
    ...

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "author_id") // @JoinColumn을 사용하면 매핑 테이블을 사용하지 않고도 foreign key를 매핑한다.
    private List<Book> books = new ArrayList<>();

    public void addBooks(Book... books) {
        for (Book book : books) {
            this.books.add(book);
        }
    }
    ...
}
```

```sql
# Author Entity에 @JoinColumn 사용

Insert one author with three books  ...
# author 저장
Hibernate: 
    insert 
    into
        author
        (age, genre, name, id) 
    values
        (?, ?, ?, default)

# book 저장
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
  
# book 엔티티에 author_id 설정
Hibernate:
update
    book
set
    author_id=?
where
    id=?
Hibernate:
update
    book
set
    author_id=?
where
    id=?
Hibernate:
update
    book
set
    author_id=?
where
    id=?
```

📌 **비교 포인트:**
`@JoinColumn`을 사용하면 더 이상 author_books 매핑 테이블이 사용되지 않고 book 엔티티에 foreign key를 저장하는 것을 확인할 수 있다.
대신 book 엔티티에 author_id 컬럼을 저장해야 하기 때문에 author_books 매핑 테이블에 데이터를 추가하는 대신 book 엔티티에 update 쿼리가 실행된다.
도서 추가/삭제 시에도 author_id 컬럼을 모두 null로 update한 후 다시 author_id를 설정하는 쿼리가 실행된다.

## @OrderColumn + List 사용

```java
@Entity
@Table(name = "author")
public class Author {
    ...

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "books_order")
    private List<Book> books = new ArrayList<>();

    public void addBooks(Book... books) {
        for (Book book : books) {
            this.books.add(book);
        }
    }
    ...
}
```

```sql
# Author Entity에 @OrderColumn + List 사용
Insert one author with three books  ...
# author 저장
Hibernate: 
    insert 
    into
        author
        (age, genre, name, id) 
    values
        (?, ?, ?, default)

# book 저장
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)

# author_books 저장, books_order 설정
Hibernate: 
    insert 
    into
        author_books
        (author_id, books_order, books_id) 
    values
        (?, ?, ?)
Hibernate: 
    insert 
    into
        author_books
        (author_id, books_order, books_id) 
    values
        (?, ?, ?)
Hibernate: 
    insert 
    into
        author_books
        (author_id, books_order, books_id) 
    values
        (?, ?, ?)
```

📌 **비교 포인트:**
`@OrderColumn`을 사용하면 author_books 테이블에 추가로 books_order 컬럼이 저장되며 이 컬럼값으로 정렬합니다.
도서를 추가할 경우에는 단순히 book 엔티티를 추가하고 author_books에도 추가하면 됩니다.
하지만 도서를 삭제할 경우에는 삭제한 도서의 위치에 따라 다르게 동작합니다.
- 마지막 도서 삭제: author_books 테이블에 있는 마지막 도서 매핑 정보를 삭제하고 book 엔티티도 삭제
- 첫번째/중간 도서 삭제: author_books 테이블에 있는 삭제한 도서의 이후 순번 도서들의 books_order를 재정렬하기 위한 update 쿼리 실행 후 book 엔티티 삭제


## @OrderColumn + Set 사용

```java
@Entity
@Table(name = "author")
public class Author {
    ...

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "books_order")
    private Set<Book> books = new HashSet<>();

    public void addBooks(Book... books) {
        for (Book book : books) {
            this.books.add(book);
        }
    }
    ...
}

@Entity
@Table(name = "book")
public class Book {
    ...

    // collection을 Set으로 할 경우 equals와 hashCode를 재정의해야 한다.
    // 그렇지 않고 id만을 사용할 경우 모두 같은 객체라고 판단하여 저장이 되지 않는다.
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(id, book.id) && Objects.equals(title, book.title) && Objects.equals(isbn, book.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, isbn);
    }
}
```

```sql
# Author Entity에 @OrderColumn + Set 사용
Insert one author with three books  ...
# author 저장
Hibernate: 
    insert 
    into
        author
        (age, genre, name, id) 
    values
        (?, ?, ?, default)

# book 저장
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)
Hibernate: 
    insert 
    into
        book
        (isbn, title, id) 
    values
        (?, ?, default)

# author_books 저장, books_order 설정
Hibernate: 
    insert 
    into
        author_books
        (author_id, books_id) 
    values
        (?, ?)
Hibernate: 
    insert 
    into
        author_books
        (author_id, books_id) 
    values
        (?, ?)
Hibernate: 
    insert 
    into
        author_books
        (author_id, books_id) 
    values
        (?, ?)
```

📌 **비교 포인트:**
도서를 추가할 때는 book 엔티티에 equals()와 hashCode() 메서드를 재정의해야 하며, 이 때 다른 값을 가질 수 있는 필드들을 비교해야 한다. (그렇지 않으면 모두 같은 객체라고 판단하여 데이터를 저장하지 않는다.)
도서를 추가할 경우에는 List와 마찬가지로 단순히 book 엔티티를 추가하고 author_books에도 추가하면 됩니다.
도서를 삭제할 경우에는 author_books의 데이터를 삭제한 후 book 엔티티를 삭제합니다.
 - List와 달리 books_order를 재정렬하기 위한 update 쿼리가 실행되지 않습니다.

---

## 💬 나의 코멘트
`@OrderColumn`은 JPA 스펙에 있기 때문에 언급은 되지만 실무에서는 잘 사용하지 않는다고 하며 되도록 사용하지 말라고까지 한다.
그리고 `@JoinColumn`은 매핑 테이블을 추가로 만들고 싶지 않은 경우에는 유용하게 사용할 수 있지만 만약 의도적으로 매핑 테이블이 필요하다면 `@JoinColumn`을 사용하지 않는 것이 좋다.
단방향 연관관계는 양방향 보다 외래키 매핑과 관련하여 쿼리가 추가로 발생하여 성능상 이슈가 있을 수 있기 때문에 주의해야 한다.

되도록 단방향 연관관계보다는 양방향으로 설정하는 것이 더 좋다.

---

## 📂 참고할만한 자료
