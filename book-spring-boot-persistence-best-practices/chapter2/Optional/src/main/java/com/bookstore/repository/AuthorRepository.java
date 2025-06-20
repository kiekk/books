package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByName(String name);

    @Query("SELECT a FROM Author a WHERE a.name=:name")
    Optional<Author> fetchByName(@Param("name") String name);

    @Query("SELECT a.genre FROM Author a WHERE a.name=:name")
    Optional<String> fetchGenreByName(@Param("name") String name);

    @Query(value = "SELECT a.genre FROM author a WHERE a.name=:name", nativeQuery = true)
    Optional<String> fetchGenreByNameNative(@Param("name") String name);
}
