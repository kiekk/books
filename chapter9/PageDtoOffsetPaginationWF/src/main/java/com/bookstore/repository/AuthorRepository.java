package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AuthorRepository extends PagingAndSortingRepository<Author, Long> {
}
