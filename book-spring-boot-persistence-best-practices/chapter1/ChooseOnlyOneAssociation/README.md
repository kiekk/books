# 📘 [연관관계] - 단 하나의 연관관계만 Null이 아닌지 확인하는 방법

---

## 📖 전반적인 내용

이 장에서는 **[단 하나의 연관관계만 Null이 아닌지 확인하는 방법]** 에 대해 학습합니다.

- Custom Annotation을 사용해서 연관관계 객체들의 유효성 검증을 할 수 있습니다.
- 네이티브 쿼리를 사용할 경우 애플리케이션의 유효성 검사가 무시될 수 있는데 이는 Trigger를 통해서 해결할 수 있습니다.

---

## 🔍 중심 로직 캡처

```java
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {JustOneOfManyValidator.class})
public @interface JustOneOfMany {
  String message() default "A review can be associated with either a book, a magazine or an article";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
```

```java
public class JustOneOfManyValidator implements ConstraintValidator<JustOneOfMany, Review> {
    @Override
    public boolean isValid(Review review, ConstraintValidatorContext ctx) {
        return Stream.of(review.getBook(), review.getArticle(), review.getMagazine())
                .filter(Objects::nonNull)
                .count() == 1;
    }
}
```

```java
@Entity
@JustOneOfMany
public class Review {
  ...
}
```

---

## 💬 나의 코멘트
- Custom Annotation을 사용하여 연관관계 객체들의 유효성 검증을 할 수는 있겠지만 이런 기능을 실제로 사용한 적은 본 적이 없어서 실사용률은 낮을 것 같습니다.

---
