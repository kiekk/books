# 📘 [연관관계] - @OneToMay 연관관계를 효과적으로 구성하는 방법

---

## 📖 전반적인 내용

이 장에서는 **[@OneToMay 연관관계를 효과적으로 구성하는 방법]** 에 대해 학습합니다.

- 1 <-> N 연관관계 설정은 특별한 이유가 있지 않는 한 양방향 연관관계로 설정하는 것이 좋다.
- 양방향 연관관계 설정 시 연관관계 주인이 아닌 객체는 mappedBy 속성을 이용하여 연관관계 주인을 설정해야 한다.
- 연관관계 주인은 @JoinColumn을 사용하여 매핑할 컬럼을 지정할 수 있다.
- CascadeType 속성을 이용하여 연관된 엔티티의 상태 변화에 대한 전파 규칙을 설정할 수 있다.
- CascadeType.ALL 속성을 사용하면 Author 엔티티의 영속 상태에 따라 연관된 Book 엔티티도 자동으로 관리한다는 뜻이다.
- orphanRemoval 속성을 이용하여 비즈니스 로직, 객체 상태에서 삭제할 경우 자동으로 DB에서도 삭제되도록 설정할 수 있다.
---

## 🔍 중심 로직 캡처

다음은 이 장에서 핵심적으로 다룬 코드입니다:

```java
@Service
public class BookstoreService {
    ...
    public void insertOtherAuthorWithBooks() {
        Author author = Author.createAuthor("Joana Nimar", "History", 34);

        Book book01 = Book.createBook("001-JN", "A History of Ancient Prague");
        Book book02 = Book.createBook("002-JN", "A People's History");
        Book book03 = Book.createBook("003-JN", "A People's History");

        author.addBooks(book01, book02, book03); // use addBooks() helper

        // * author 객체만 저장했을 뿐인데 CascadeType.ALL 속성으로 인해 book 엔티티도 함께 저장됨
        authorRepository.save(author);
    }
}

@Entity
@Table(name = "author")
public class Author {
    ...
    public void addBooks(Book... books) {
        for (Book book : books) {
            this.books.add(book);
            book.setAuthor(this); // book 엔티티에도 author 엔티티를 설정해야 자동으로 author 엔티티의 id가 저장됨
        }
    }
}

```

📌   
CascadeType.ALL 타입을 사용했기 때문에 `author` 엔티티만 저장하면 `book` 엔티티는 자동으로 함께 저장됩니다.
여기서 중요한 것은 author.addBooks() 메서드를 통해 book 엔티티를 추가할 때 book 엔티티에도 author 엔티티를 설정해야 자동으로 author 엔티티의 id가 저장된다는 점입니다.

---

```java
public interface AuthorRepository extends JpaRepository<Author, Long> {
    @Query("SELECT a FROM Author a JOIN FETCH a.books WHERE a.name = :name")
    Author fetchByName(@Param("name") String name);
}
```

📌   
CascadeType.ALL 타입을 사용했기 때문에 `author` 엔티티만 저장하면 `book` 엔티티는 자동으로 함께 저장됩니다.
여기서 중요한 것은 author.addBooks() 메서드를 통해 book 엔티티를 추가할 때 book 엔티티에도 author 엔티티를 설정해야 자동으로 author 엔티티의 id가 저장된다는 점입니다.
---

## 🧪 쿼리 비교

| 구분            | 실행 쿼리                                                                   |
|---------------|-------------------------------------------------------------------------|
| 책             | `select m from Member m where m.username = ?1`                          |
| 변경            | `select m.id, m.username from Member m where m.username = :username`    |

📌 **비교 포인트:**
- ✅ 코드 가독성과 유지보수를 위해 위치 기반 파라미터 설정인 `?1` 대신 이름 기반 파라미터 설정인 `:username`을 사용한다.

---

## 💬 나의 코멘트

- 1 <-> N 연관관계를 설정할 경우 특별한 이유가 없는 한 양방향 연관관계로 설정하는 것이 좋다.
- 그리고 1 <-> N 연관관계 설정 시 만약 부모 엔티티가 자식 엔티티의 영속 상태를 전부 관리한다면 CascadeType.ALL + orphanRemoval = true속성을 같이 사용하여 자식 엔티티의 영속 상태를 관리하는 것이 좋다.
  - 그렇지 않다면 CascadeType.ALL + orphanRemoval = true 속성을 같이 사용하는 것은 오동작할 가능성이 있어 위험하다.
- 예제는 Entity를 생성할 떄 기본 생성자 + setter 방식을 택했는데 이는 캡슐화가 어긋난다고 판단하여 정적 팩토리 메서드를 사용하여 객체를 생성하는 방식으로 변경했다.
- 예제는 mysql을 사용했지만 간단하게 테스트를 하며 학습하는 것이 목적이기 떄문에 인메모리 DB인 H2로 변경한다.
- toString() 메서드에서는 해당 엔티티에 있는 필드들만 출력하도록 오버라이딩을 했다.
---

## 📂 참고할만한 자료

- JPA 메서드에 파라미터 바인딩 시 에러 발생
  - [link](https://velog.io/@ghwns9991/%EC%8A%A4%ED%94%84%EB%A7%81-%EB%B6%80%ED%8A%B8-3.2-%EB%A7%A4%EA%B0%9C%EB%B3%80%EC%88%98-%EC%9D%B4%EB%A6%84-%EC%9D%B8%EC%8B%9D-%EB%AC%B8%EC%A0%9C)
  - [link](https://www.korecmblog.com/blog/upgrade-tospring6.1-parameter-name-retention)
- CascadeType.REMOVE vs orphanRemoval = true
  - [link](https://kobumddaring.tistory.com/56)
  - [link](https://velog.io/@yuseogi0218/JPA-CascadeType.REMOVE-vs-orphanRemoval-true)
  - [link](https://pjs-world.tistory.com/entry/JPA-%EC%98%81%EC%86%8D%EC%84%B1-%EC%A0%84%EC%9D%B4CASCADE%EC%99%80-%EA%B3%A0%EC%95%84-%EA%B0%9D%EC%B2%B4%EB%B6%80%EB%AA%A8-%EC%97%94%ED%8B%B0%ED%8B%B0%EB%A5%BC-%ED%86%B5%ED%95%B4-%EC%9E%90%EC%8B%9D%EC%9D%98-%EC%83%9D%EB%AA%85%EC%A3%BC%EA%B8%B0-%EA%B4%80%EB%A6%AC)
