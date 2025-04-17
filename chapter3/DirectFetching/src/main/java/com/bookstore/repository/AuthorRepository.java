package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    @Query("SELECT a FROM Author a WHERE a.id = :id")
    @Override
    Optional<Author> findById(@Param("id") Long id);

    @Query("SELECT a FROM Author a WHERE a.id = :id")
    Optional<Author> fetchById(@Param("id") Long id);

    @Query("SELECT a FROM Author a WHERE a.id = :id")
    Author fetchByIdJpql(@Param("id") Long id);

    @Query(value = "SELECT * FROM author WHERE id = :id", nativeQuery = true)
    Author fetchByIdSql(@Param("id") Long id);

    @Query("SELECT a.name FROM Author a WHERE a.id = :id")
    String fetchNameByIdJpql(@Param("id") Long id);

    @Query(value = "SELECT name FROM author WHERE id = :id", nativeQuery = true)
    String fetchNameByIdSql(@Param("id") Long id);
}
