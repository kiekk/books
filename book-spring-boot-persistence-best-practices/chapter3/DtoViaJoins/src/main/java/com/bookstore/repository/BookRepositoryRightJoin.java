package com.bookstore.repository;

import com.bookstore.dto.AuthorNameBookTitle;
import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepositoryRightJoin extends JpaRepository<Book, Long> {
    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM Book b RIGHT JOIN b.author a")
    List<AuthorNameBookTitle> findBooksAndAuthorsJpql();

    @Query(value = "SELECT b.title AS title, a.name AS name "
            + "FROM book b RIGHT JOIN author a ON a.id = b.author_id",
            nativeQuery = true)
    List<AuthorNameBookTitle> findBooksAndAuthorsSql();
}
