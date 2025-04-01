package com.bookstore.repository;

import com.bookstore.entity.BookList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookListRepository extends JpaRepository<BookList, Long> {
}
