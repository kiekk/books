package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author,Long> {
    @Query(value = "SELECT * FROM author", nativeQuery = true)
    List<Author> findAllIncludingDeleted();

    @Query(value = "SELECT * FROM author AS a WHERE a.deleted = true", nativeQuery = true)
    List<Author> findAllOnlyDeleted();

    @Query(value = "UPDATE Author a SET a.deleted = false WHERE a.id = ?1")
    @Modifying
    void restoreById(Long id);
}
