package com.bookstore.repository;

import com.bookstore.dto.AuthorDto;
import com.bookstore.entity.Author;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface AuthorRepository extends PagingAndSortingRepository<Author, Long> {
    @Query(value = "SELECT name, age, COUNT(*) OVER() AS total FROM author",
            nativeQuery = true)
    List<AuthorDto> fetchAll(Pageable pageable);
}
