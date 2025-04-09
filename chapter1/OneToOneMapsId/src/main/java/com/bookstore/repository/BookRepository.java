package com.bookstore.repository;

import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT b FROM Book b WHERE b.author = :author")
    Book fetchBookByAuthor(@Param("author") Author author);
}
