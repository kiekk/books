# 📘 [연관관계] - 엔터티 그래프 및 기본 속성 처리 방법

---

## 📖 전반적인 내용

이 장에서는 **[엔터티 그래프 및 기본 속성 처리 방법]** 에 대해 학습합니다.

- JPA 기반으로 엔터티 그래프를 사용해 엔터티의 일부 기본 속성(basic attributes)만을 가져오려면 @Basic(fetch = FetchType.LAZY)로
엔터티 그래프의 일부가 아닌 기본 속성을 지정합니다.
- 해당 설정이 적용되기 위해서는 하이버네이트 Bytecode Enhancement가 활성화 되어야 합니다.
- 주의할 점은 엔터티 그래프 쿼리뿐만 아니라 모든 쿼리들도 지연 로딩으로 처리됩니다.

---

## 🔍 중심 로직 캡처

다음은 이 장에서 핵심적으로 다룬 코드입니다:

```groovy
// Bytecode Enhancement 활성화
// build.gradle
plugins {
    ...
    id("org.hibernate.orm") version "6.5.2.Final"
}

...

hibernate {
  enhancement {
    enableLazyInitialization = true
  }
}
```

```java
// Author.java
@Entity
public class Author {
    
    ...
  
    @Basic(fetch = FetchType.LAZY)
    private String genre;

    @Basic(fetch = FetchType.LAZY)
    private int age;
    
    ...
}
```

📌
- `@Basic(fetch = FetchType.LAZY)`를 사용하여 엔터티 그래프의 일부가 아닌 기본 속성을 지정합니다.

---

## 💬 나의 코멘트
- `@Basic(fetch = FetchType.LAZY)`를 사용하여 엔터티의 기본 속성 중 일부를 지연 로딩으로 설정할 수 있습니다.
- BLOB, CLOB 타입의 속성은 매번 로딩 시 성능 이슈가 있을 수 있기 때문에 해당 속성을 지연 로딩으로 설정하면 좋을 것 같습니다.

---

## 📂 참고할만한 자료
