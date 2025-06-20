package com.bookstore.repository;

import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query(value = "SELECT b FROM Book b LEFT JOIN FETCH b.author")
        // or, via JOIN
        // @Query(value = "SELECT b, a FROM Book b LEFT JOIN b.author a")
    List<Book> fetchBookWithAuthor();
}
