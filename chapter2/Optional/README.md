# 📘 [엔터티] - 영속성 레이어에서 자바 8 Optional 사용 방법

---

## 📖 전반적인 내용

이 장에서는 **[영속성 레이어에서 자바 8 Optional 사용 방법]** 에 대해 학습합니다.

- 엔터티의 getter에 `Optional`을 사용할 수 있습니다.
- 또는 Repository에서 메서드의 반환 타입으로도 Optional을 사용할 수 있습니다.
- 반면 다음과 같은 경우에는 `Optional`을 사용하지 않는 것이 좋습니다.
  - 엔터티의 필드에 `Optional`을 사용하는 경우 (`Optional`은 Serializable 되지 않음)
  - 엔터티의 setter에 `Optional`을 사용하는 경우
  - 엔터티의 생성자에 `Optional`을 사용하는 경우
  - 기본 타입 및 컬렉션을 반환하는 getter
  - 기본키에 관련된 getter

---

## 🔍 중심 로직 캡처

```java
@Entity
public class Author {
    
    ...
    
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getGenre() {
        return Optional.ofNullable(genre);
    }
    
}
```

```java
@Entity
public class Book {
    
    ...

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public Optional<String> getIsbn() {
        return Optional.ofNullable(isbn);
    }

    public Optional<Author> getAuthor() {
        return Optional.ofNullable(author);
    }
}
```

---

## 💬 나의 코멘트
- `Optional`을 사용하면 null을 안전하게 다룰 수 있다는 장점이 있긴 하지만 매번 null 체크를 해야 하기 때문에 코드가 복잡해질 수 있습니다.
- 개인적으로는 `Optional`을 사용하기보다는 null 체크를 통해 처리하는 것이 더 가독성이 좋다고 생각합니다.
- `Optional`을 사용하기 전에 `Optional`이 정말 필요한지 한 번 더 고민해보는 것이 좋습니다.

---
