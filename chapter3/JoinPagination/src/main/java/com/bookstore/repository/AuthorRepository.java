package com.bookstore.repository;

import com.bookstore.dto.AuthorBookDto;
import com.bookstore.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    @Query(value = "SELECT a.name AS name, a.age AS age, b.title AS title, b.isbn AS isbn"
            + " FROM Author a LEFT JOIN a.books b WHERE a.genre = :genre")
    Page<AuthorBookDto> fetchPageOfDto(@Param("genre") String genre, Pageable pageable);
}
