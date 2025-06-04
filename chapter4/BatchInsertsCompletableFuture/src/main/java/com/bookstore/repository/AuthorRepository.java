package com.bookstore.repository;

import com.bookstore.entity.Author;
import com.bookstore.impl.BatchRepository;

public interface AuthorRepository extends BatchRepository<Author, Long> {
}
