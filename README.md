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
2. JPA로 CRUD 기능 구현하여 JSP 페이지로 출력하기
3. Thymeleaf를 이용하여 게시판 페이지, CRUD 기능 구현
4. 로그인, 로그아웃 기능 구현, 로그인 상태 유지 구현
5. 예외 처리, 각 예외마다 해당하는 예외 페이지 연결
6. Spring Security를 적용, 페이지별로 접근 권한 설정하여 로그인한 사용자의 자원 접근 제한
7. JPA와 연동하여 Security 구현, 필터 적용으로 인증과 인가 확인, PasswordEncoder 객체로 비밀번호 암호화 구현

> 필터 중 가장 중요한 **인증,** **인가**를 처리하는 필터 2개는 꼭 알아두면 좋겠습니다.

|필터|기능|
|--|--|
|UsernamePasswordAuthenticationFilter|인증|
|FilterSecurityInterceptor|인가|

BoardWebProject
- 날짜 : 20201.01.12
- 내용 : 이제까지 공부했던 내용들을 총 정리하며 Security, JPA, Thymeleaf등을 사용하여 게시판 프로그램을 만들어봤습니다.

### 구현한 기능 or 공부한 내용
###### [x] : Spring Boot로 간편하게 라이브러리들을 관리
###### [x] : PasswordEncoder 객체를 사용하여 간편하게 비밀번호 암호화
###### [x] : application.properties 파일을 통해 설정 정보 관리
###### [x] : Thymeleaf를 사용하여 View 페이지 구현
###### [x] : H2 데이터베이스를 사용, JPA 연동
###### [x] : Search 객체를 생성하여 게시판 검색 기능을 동적 쿼리로 구현, QueryDSL
###### [x] : 회원별로 권한을 부여하고 SpringSecurity로 인증, 인가 기능 구현
