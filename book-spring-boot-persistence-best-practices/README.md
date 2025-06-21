# 📘 [스프링 부트 JPA 모범 사례]

> [스프링 부트 JPA 모범 사례]의 예제 코드를 직접 구현하며 학습한 내용을 정리한 공간입니다.

<img src="thumbnail.jpg" width="300"/>

<sub>※ 이미지 출처: 교보문고</sub>

#### 정보

- 제목: 스프링 부트 JPA 모범 사례
- 저자: 안겔 레오나르드
- [교보문고 바로 가기](https://product.kyobobook.co.kr/detail/S000212718293)

---

## 📚 목차 및 소스코드 링크

> 각 장의 디렉토리에는 해당 챕터에서 다루는 코드와 실습 결과가 정리되어 있습니다.

<details>
<summary>1장. 연관관계</summary>
<div markdown="1">

| NO | 항목                                                                    | 소스코드 경로                                                                                 |
|----|-----------------------------------------------------------------------|-----------------------------------------------------------------------------------------|
| 1  | @OneToMay 연관관계를 효과적으로 구성하는 방법                                         | [/chapter1/OneToManyBidirectional](chapter1/OneToManyBidirectional)                     |
| 2  | 단방향 @OneToMany 연관관계를 피해야 하는 이유                                        | [/chapter1/OneToManyUnidirectional](chapter1/OneToManyUnidirectional)                   |
| 3  | 단방향 @ManyToOne의 효율성                                                   | [/chapter1/JustManyToOne](chapter1/JustManyToOne)                                       |
| 4  | @ManyToMany 연관관계를 효과적으로 구성하는 방법                                       | [/chapter1/ManyToManyBidirectional](chapter1/ManyToManyBidirectional)                   |
| 5  | @ManyToMany에서 Set이 List보다 나은 이유                                       | [/chapter1/ManyToManyBidirectionalListVsSet](chapter1/ManyToManyBidirectionalListVsSet) |
| 6  | CascadeType.REMOVE 및 orphanRemoval=true를 사용해 하위 엔터티 제거를 피해야 하는 이유와 시기 | [/chapter1/CascadeChildRemoval](chapter1/CascadeChildRemoval)                           |
| 7  | JPA 엔터티 그래프를 통해 연관관계를 가져오는 방법                                         | [/chapter1/EntityGraphAttributePaths](chapter1/EntityGraphAttributePaths)               |
| 8  | JPA 엔터티 서브그래프를 통해 연관관계를 가져오는 방법                                       | [/chapter1/NamedSubgraph](chapter1/NamedSubgraph)                                       |
| 9  | 엔터티 그래프 및 기본 속성 처리 방법                                                 | [/chapter1/NamedEntityGraphBasicAttrs](chapter1/NamedEntityGraphBasicAttrs)             |
| 10 | 하이버네이트 @Where 어노테이션을 통한 연관관계 필터링 처리                                   | [/chapter1/FilterAssociation](chapter1/FilterAssociation)                               |
| 11 | @MapsId를 통한 단방향/양방향 @OneToOne 최적화 방법                                  | [/chapter1/OneToOneMapsId](chapter1/OneToOneMapsId)                                     |
| 12 | 단 하나의 연관관계만 Null이 아닌지 확인하는 방법                                         | [/chapter1/ChooseOnlyOneAssociation](chapter1/ChooseOnlyOneAssociation)                 |

</div>
</details>
<br />
<details>
<summary>2장. 엔터티</summary>
<div markdown="1">

| NO | 항목                                | 소스코드 경로                                   |
|----|-----------------------------------|-------------------------------------------|
| 13 | 엔터티의 플루언트 API 스타일 적용 방법           | [/chapter2/FluentApi](chapter2/FluentApi) |
| 14 | 하이버네이트 프록시를 통한 자식 측에서 부모 연관관계 채우기 | [/chapter2/PopulatingChildViaProxy](chapter2/PopulatingChildViaProxy) |
| 15 | 영속성 레이어에서 자바 8 Optional 사용 방법 | [/chapter2/Optional](chapter2/Optional) |

</div>
</details>

---

## 🛠️ 환경 정보

- Java: `21`
- Spring Boot: `3.4.4` ~ `3.5.0`
- Build Tool: `Gradle 8.13` ~ `Gradle 8.14`
- IDE: `IntelliJ`

---

## 🗂️ 정리 방식

- 각 장마다 별도 디렉토리/프로젝트로 구분
- 실습 예제 코드 + 간단한 요약 README 포함
- 일부 코드는 수정되었거나 리팩토링되어 책과 다를 수 있음

---

