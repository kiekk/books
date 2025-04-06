package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    @EntityGraph(value = "author-books-graph", type = EntityGraph.EntityGraphType.FETCH)
    List<Author> findByAgeGreaterThanAndGenre(int age, String genre);

    @EntityGraph(value = "author-books-graph", type = EntityGraph.EntityGraphType.LOAD)
    List<Author> findByGenreAndAgeGreaterThan(String genre, int age);
}
