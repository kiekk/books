package com.bookstore.repository;

import com.bookstore.dto.AuthorDto;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    @Query(nativeQuery = true)
    List<String> fetchName();

    @Query(nativeQuery = true)
    List<AuthorDto> fetchNameAndAge();
}
