package com.bookstore.repository;

import com.bookstore.dto.AuthorNameBookTitle;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthorRepositoryInnerJoin extends JpaRepository<Author, Long> {
    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM Author a INNER JOIN a.books b")
    List<AuthorNameBookTitle> findAuthorsAndBooksJpql();

    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM author a INNER JOIN book b ON a.id = b.author_id",
            nativeQuery = true)
    List<AuthorNameBookTitle> findAuthorsAndBooksSql();

    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM Author a INNER JOIN a.books b "
            + "WHERE a.genre = :genre AND b.price < :price")
    List<AuthorNameBookTitle> findAuthorsAndBooksByGenreAndPriceJpql(@Param("genre") String genre,
                                                                     @Param("price") int price);

    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM author a INNER JOIN book b ON a.id = b.author_id WHERE a.genre = :genre AND b.price < :price",
            nativeQuery = true)
    List<AuthorNameBookTitle> findAuthorsAndBooksByGenreAndPriceSql(@Param("genre") String genre,
                                                                    @Param("price") int price);
}
