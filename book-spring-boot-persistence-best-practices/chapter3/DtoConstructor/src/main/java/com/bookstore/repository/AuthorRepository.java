package com.bookstore.repository;

import com.bookstore.dto.AuthorDto;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    List<AuthorDto> findByGenre(String genre);

    @Query(value = "SELECT new com.bookstore.dto.AuthorDto(a.name, a.age) FROM Author a")
    List<AuthorDto> fetchAuthors();
}
