package com.bookstore.repository;

import com.bookstore.entity.BookSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookSetRepository extends JpaRepository<BookSet, Long> {
}
