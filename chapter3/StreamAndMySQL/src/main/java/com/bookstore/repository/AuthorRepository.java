package com.bookstore.repository;

import com.bookstore.dto.AuthorName;
import com.bookstore.entity.Author;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.util.Streamable;

import java.util.stream.Stream;

import static org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    @Query("SELECT a FROM Author a")
    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "" + Integer.MIN_VALUE))
    Stream<Author> streamAll();

    @Query("SELECT a FROM Author a")
    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "30"))
    Stream<Author> streamAll2();

    Streamable<Author> findByGenre(String genre);

    Streamable<Author> findByAgeGreaterThan(int age);

    Streamable<AuthorName> queryByGenre(String genre);
}
