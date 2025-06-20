package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    /*
    Count 쿼리 누락으로 인한 QueryException 발생
    @Transactional(readOnly = true)
    @Query(value = "SELECT a FROM Author a JOIN FETCH a.books WHERE a.genre = ?1")
    Page<Author> fetchWithBooksByGenre (String genre, Pageable pageable);
    //*/

    @Transactional
    @Query(value = "SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.genre = ?1",
            countQuery = "SELECT COUNT(a) FROM Author a WHERE a.genre = ?1")
    Page<Author> fetchWithBooksByGenreCQ(String genre, Pageable pageable);

    @Transactional
    @EntityGraph(attributePaths = {"books"},type = EntityGraph.EntityGraphType.FETCH)
    @Query(value = "SELECT a FROM Author a WHERE a.genre = ?1")
    Page<Author> fetchWithBooksByGenreEG(String genre, Pageable pageable);
}
