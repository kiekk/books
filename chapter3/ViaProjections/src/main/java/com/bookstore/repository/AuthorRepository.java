package com.bookstore.repository;

import com.bookstore.entity.Author;
import com.bookstore.projection.AuthorNameAge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    List<AuthorNameAge> findFirst2ByGenre(String genre);

    @Query(value = "SELECT a.name AS name, a.age AS age FROM Author a WHERE a.genre=:genre")
    List<AuthorNameAge> fetchByGenre(@Param("genre") String genre);

    @Query(value = "SELECT a.name, a.age FROM author a WHERE a.genre=:genre LIMIT 2",
            nativeQuery = true)
    List<AuthorNameAge> fetchByGenreLimit2(@Param("genre") String genre);

    @Query(value = "SELECT a.author_name AS name, a.author_age AS age " +
            "FROM author a WHERE a.genre=:genre LIMIT 2",
            nativeQuery = true)
    List<AuthorNameAge> fetchByGenreLimit2WithAlias(@Param("genre") String genre);

    // Scalar Mapping
    List<String> fetchName();

    // Spring projection
    List<AuthorNameAge> fetchNameAndAge();
}
