package com.bookstore.repository;

import com.bookstore.dto.AuthorNameAge;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    @Query("SELECT a.name AS name, a.age AS age FROM Author a WHERE a.age >= :age")
    List<AuthorNameAge> fetchByAge(@Param("age") int age);
}
