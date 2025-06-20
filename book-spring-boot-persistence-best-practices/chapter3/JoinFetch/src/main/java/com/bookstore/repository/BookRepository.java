package com.bookstore.repository;

import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query(value = "SELECT b FROM Book b JOIN FETCH b.author WHERE b.isbn = :isbn")
        // or, via JOIN
        //@Query(value = "SELECT b, a FROM Book b JOIN b.author a WHERE b.isbn = :isbn")
    Book fetchBookWithAuthorByIsbn(@Param("isbn") String isbn);
}
