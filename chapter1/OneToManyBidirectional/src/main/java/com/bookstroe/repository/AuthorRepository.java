package com.bookstroe.repository;

import com.bookstroe.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    // 책에서는 위치 기반 파라미터 설정이지만 명확하게 이름 기반 파라미터 설정을 사용한다.
    @Query("SELECT a FROM Author a JOIN FETCH a.books WHERE a.name = :name")
    Author fetchByName(@Param("name") String name);
    // spring boot 3.0 부터 @Param을 설정하지 않으면 에러가 발생한다.
    // Caused by: java.lang.IllegalStateException: For queries with named parameters you need to provide names for method parameters; Use @Param for query method parameters, or when on Java 8+ use the javac flag -parameters
}
