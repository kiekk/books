package com.bookstore.repository;

import com.bookstore.entity.Book;
import com.bookstore.wrapper.Books;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    Books findBy();
}
