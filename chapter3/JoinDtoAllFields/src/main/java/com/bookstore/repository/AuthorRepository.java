package com.bookstore.repository;

import com.bookstore.dto.AuthorDto;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    List<AuthorDto> findBy();
//    @Query("SELECT a.id, a.age, a.name, a.genre FROM Author a") // 엔터티 컬럼 조회
//    List<Object[]> findBy();

    @Query("SELECT a FROM Author a")
    List<Object[]> fetchAsArray();
}
