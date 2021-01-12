# SpringBoot
SpringBoot로 게시판 만들기

## 소개
SpringBoot로 빠르게 게시판을 만들어봅니다.
UI보다는 Business, Persistence에 집중하고, SpringSecurity나 JPA와 같은 기술들도 공부합니다.

## 참고 서적
[스프링 부트 퀵스타트](http://www.kyobobook.co.kr/product/detailViewKor.laf?ejkGb=KOR&mallGb=KOR&barcode=9791186710487&orderClick=LAG&Kc=)

## 목적
1. Spring으로 만들었던 게시판을 복습
2. SpringBoot 공부
3. SpringSecurity, JPA 기술 적용

## 작업일

day01
- 날짜 : 2021.01.09
- 내용 : SpringBoot 환경 구축 및 프로젝트 생성 후 포트 설정

day02
- 날짜 : 2021.01.10
- 내용 : BoardController 생성 후 @RestController 적용

day03
- 날짜 : 2021.01.11
- 내용 : 
1. 어노테이션을 이용해 JDBCConnectionManager 객체 자동 생성하여 DB 연결
2. properties파일을 이용해 JDBC 연결
3. 테스트 코드 작성, MockMvc를 사용하여 간단하게 컨트롤러를 테스트
4. application.properties파일과 xml파일을 이용해서 로깅 설정하기
5. jar로 빌드 (Run As -> Maven install)
6. H2 데이터베이스로 JPA 연동 및 테이블 생성 후 insert, select 기능 구현
7. delete, update, select list 기능 구현
8. Spring Boot로 Spring Data JPA에서 제공하는 CrudRepository로 CRUD 기능 구현
9. 쿼리 메소드 사용
```
find + 엔티티 이름 + By + 변수 이름
ex) findBoardByTitle() : Board 엔티티에서 title 변수 값만 조회합니다.

또는 엔티티를 생략할 수도 있습니다.

findBy + 변수 이름
ex) findByTitle() : Board 엔티티에서 title 변수 값만 조회합니다.

```
10. @Query 어노테이션으로 JPQL, 네이티브 쿼리 사용

day04
- 날짜 : 2021.01.12
- 내용 : 
1. QueryDSL을 이용하여 동적 쿼리 적용, 객체 간 관계 설정