package com.bookstore.repository;

import com.bookstore.dto.AuthorDto;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    List<Author> findByAgeGreaterThanEqual(int age);

    @Query("SELECT a.name AS name, a.avatar AS avatar FROM Author a WHERE a.age >= :age")
    List<AuthorDto> findDtoByAgeGreaterThanEqual(@Param("age") int age);
}
