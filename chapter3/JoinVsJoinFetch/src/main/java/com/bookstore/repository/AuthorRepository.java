package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    // INNER JOIN
    @Query(value = "SELECT a, b FROM Author a INNER JOIN a.books b WHERE b.price > :price")
//    @Query(value = "SELECT a FROM Author a INNER JOIN a.books b WHERE b.price > :price")
    List<Author> fetchAuthorsBooksByPriceInnerJoin(@Param("price") int price);

    // JOIN FETCH
    @Query(value = "SELECT a FROM Author a JOIN FETCH a.books b WHERE b.price > :price")
    List<Author> fetchAuthorsBooksByPriceJoinFetch(@Param("price") int price);
}
