package com.bookstore.repository;

import com.bookstore.dto.AuthorNameBookTitle;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepositoryFullJoin extends JpaRepository<Author, Long> {
    @Query(value = "(SELECT b.title AS title, a.name AS name FROM author a "
            + "LEFT JOIN book b ON a.id = b.author_id) "
            + "UNION "
            + "(SELECT b.title AS title, a.name AS name FROM author a "
            + "RIGHT JOIN book b ON a.id = b.author_id "
            + "WHERE a.id IS NULL)",
            nativeQuery = true)
    List<AuthorNameBookTitle> findAuthorsAndBooksSql();
}
