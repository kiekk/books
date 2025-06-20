package com.bookstore.repository;

import com.bookstore.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT b FROM Book b WHERE b.author.id = :id")
    List<Book> fetchBooksOfAuthorById(@Param("id") Long id);

    @Query("SELECT b FROM Book b WHERE b.author.id = :id")
    Page<Book> fetchPageBooksOfAuthorById(@Param("id") Long id, Pageable pageable);
}
