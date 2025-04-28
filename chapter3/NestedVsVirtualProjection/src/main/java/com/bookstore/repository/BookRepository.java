package com.bookstore.repository;

import com.bookstore.dto.BookDto;
import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<BookDto> findBy();
}
