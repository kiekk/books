package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Author findByName(String name);

    List<Author> findByAge(int age);

    @Query("SELECT a FROM Author a JOIN FETCH a.books WHERE a.name = :name")
    Author findByNameWithBooks(@Param("name") String name);

    @Query("SELECT a FROM Author a JOIN FETCH a.books WHERE a.genre = :genre")
    List<Author> findByGenreWithBooks(@Param("genre") String genre);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Author a WHERE a.id = :id")
    int deleteByIdentifier(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Author a WHERE a.id IN :ids")
    int deleteBulkByIdentifier(@Param("ids") List<Long> ids);
}
