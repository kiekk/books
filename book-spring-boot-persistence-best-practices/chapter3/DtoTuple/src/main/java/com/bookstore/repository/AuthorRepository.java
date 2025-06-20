package com.bookstore.repository;

import com.bookstore.entity.Author;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    @Query(value = "SELECT a.id AS id, a.name AS name, a.age AS age FROM Author a")
//    @Query(value = "SELECT name, age FROM author", nativeQuery = true)
    List<Tuple> fetchAuthors();
}
