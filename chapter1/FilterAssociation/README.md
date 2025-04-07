# 📘 [연관관계] - 하이버네이트 @Where 어노테이션을 통한 연관관계 필터링 처리

---

## 📖 전반적인 내용

이 장에서는 **[하이버네이트 @Where 어노테이션을 통한 연관관계 필터링 처리]** 에 대해 학습합니다.

- 엔터티 필드에 @Where 어노테이션을 사용하여 연관관계 필터링 처리
- 쿼리에 적용할 필요 없이 해당 필드 로딩 시 자동으로 필터링 처리

---

## 🔍 중심 로직 캡처

다음은 이 장에서 핵심적으로 다룬 코드입니다:

```java
// Author.java
@OneToMany(cascade = CascadeType.ALL, mappedBy = "author", orphanRemoval = true)
@Where(clause = "price <= 20")
private List<Book> cheapBooks = new ArrayList<>();

// Bookstore.service
Author author = authorRepository.findById(1L).orElseThrow();
List<Book> books = author.getCheapBooks();
```

```sql
Author with cheap books ...
       
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
        a1_0.id=?

# books 조회 (where절이 적용된 상태)
Hibernate: 
    select
        cb1_0.author_id,
        cb1_0.id,
        cb1_0.isbn,
        cb1_0.price,
        cb1_0.title 
    from
        book cb1_0 
    where
        cb1_0.author_id=? 
        and (
            cb1_0.price <= 20
        )
```

---

## 💬 나의 코멘트
- `@Where`를 사용하여 엔터티에 조건을 설정한다는 것은 좋아보이지만 쿼리 분석이 어려워질 것 같다고 생각합니다.
- 책에서 추천하듯이 소프트 딜리트용으로 사용 (@Where(clause = "use = 'Y'")) 과 같은 경우에는 유용하게 사용할 수 있을 것 같습니다.

---

## 📂 참고할만한 자료
