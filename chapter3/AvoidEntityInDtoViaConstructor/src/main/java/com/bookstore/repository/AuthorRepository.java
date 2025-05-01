package com.bookstore.repository;

import com.bookstore.dto.BookstoreDto;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    /*
    책에서는 하이버네이트 5.3.9 Final 버전을 사용하기 때문에 Author 엔터티를 추가 조회하는 쿼리가 실행되는데
    이 부분은 하이버네이트 6.0에서 해결되어 현재 사용 중인 6.6.13 Final 버전에서는 추가 조회 쿼리가 발생하지 않는다.
     */
    @Query("SELECT new com.bookstore.dto.BookstoreDto(a, b.title) "
            + "FROM Author a JOIN Book b ON a.genre=b.genre ORDER BY a.id")
    List<BookstoreDto> fetchAll();
}
