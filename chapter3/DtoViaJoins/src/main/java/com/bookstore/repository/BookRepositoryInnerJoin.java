package com.bookstore.repository;

import com.bookstore.dto.AuthorNameBookTitle;
import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepositoryInnerJoin extends JpaRepository<Book, Long> {
    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM Book b INNER JOIN b.author a")
    List<AuthorNameBookTitle> findBooksAndAuthorsJpql();

    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM book b INNER JOIN author a ON a.id = b.author_id",
            nativeQuery = true)
    List<AuthorNameBookTitle> findBooksAndAuthorsSql();

    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM Book b INNER JOIN b.author a WHERE a.genre = :genre AND b.price < :price")
    List<AuthorNameBookTitle> findBooksAndAuthorsByGenreAndPriceJpql(@Param("genre") String genre,
                                                                     @Param("price") int price);

    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM book b INNER JOIN author a ON a.id = b.author_id "
            + "WHERE a.genre = :genre AND b.price < :price",
            nativeQuery = true)
    List<AuthorNameBookTitle> findBooksAndAuthorsByGenreAndPriceSql(@Param("genre") String genre,
                                                                    @Param("price") int price);
}
