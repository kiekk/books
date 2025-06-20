# 📘 [엔터티] - 엔터티의 플루언트 API 스타일 적용 방법

---

## 📖 전반적인 내용

이 장에서는 **[엔터티의 플루언트 API 스타일 적용 방법]** 에 대해 학습합니다.

---

## 🔍 중심 로직 캡처

- @Setter를 사용한 Fluent API 스타일의 엔터티
```java
@Entity
public class Author {
    
    ...

    public Author setId(Long id) {
        this.id = id;
        return this;
    }

    public Author setName(String name) {
        this.name = name;
        return this;
    }

    public Author setGenre(String genre) {
        this.genre = genre;
        return this;
    }

    public Author setAge(int age) {
        this.age = age;
        return this;
    }

    public Author setBooks(List<Book> books) {
        this.books = books;
        return this;
    }
}
```

- 별도의 메서드를 사용한 Fluent API 스타일의 엔터티
```java
@Entity
public class Author {
    
    ...

    public Author id(Long id) {
        this.id = id;
        return this;
    }

    public Author name(String name) {
        this.name = name;
        return this;
    }

    public Author genre(String genre) {
        this.genre = genre;
        return this;
    }

    public Author age(int age) {
        this.age = age;
        return this;
    }

    public Author books(List<Book> books) {
        this.books = books;
        return this;
    }
}
```

- Custom 빌더를 사용한 Fluent API 스타일의 엔터티
```java
package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Author {
    
    ...
    
    public Author() {
    }

    public Author(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.genre = builder.genre;
        this.age = builder.age;
    }

    ...

    public static class Builder {
        private Long id;
        private String name;
        private String genre;
        private int age;
        private List<Book> books = new ArrayList<>();

        public Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder age(int age) {
            this.age = age;
            return this;
        }

        public Builder books(List<Book> books) {
            this.books = books;
            return this;
        }

        public Author build() {
            return new Author(this);
        }
    }
}
```

---

## 💬 나의 코멘트
- 메서드 체이닝을 사용한 방법은 좋을 수 있으나 Setter를 사용하는 것은 객체지향적이지 않기 때문에 실제로는 Setter 방식은 사용하지 않고 빌더 패턴을 사용하는 것이 좋습니다.

---
