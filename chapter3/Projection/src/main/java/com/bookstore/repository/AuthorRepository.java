package com.bookstore.repository;

import com.bookstore.dto.AuthorDto;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    List<AuthorDto> findBy();

    @Query("SELECT a.age AS age, a.name AS name, a.genre AS genre, "
            + "a.email AS email, a.address AS address FROM Author a")
    List<AuthorDto> fetchAll();

    @Query("SELECT a.age AS age, a.name AS name, a.genre AS genre FROM Author a")
    List<AuthorDto> fetchAgeNameGenre();

    @Query("SELECT a.name AS name, a.email AS email FROM Author a")
    List<AuthorDto> fetchNameEmail();

    <T> T findByName(String name, Class<T> type);

    <T> List<T> findByGenre(String genre, Class<T> type);

    @Query("SELECT a FROM Author a WHERE a.name=:name AND a.age=:age")
    <T> T findByNameAndAge(@Param("name") String name, @Param("age") int age, Class<T> type);
}
