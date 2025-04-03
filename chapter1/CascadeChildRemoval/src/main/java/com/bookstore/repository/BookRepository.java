package com.bookstore.repository;

import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    Book findByTitle(String title);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Book b WHERE b.author.id = :id")
    int deleteByAuthorIdentifier(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Book b WHERE b.author.id IN :ids")
    int deleteBulkByAuthorIdentifier(@Param("ids") List<Long> ids);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Book b WHERE b.author IN :authors")
    int deleteBulkByAuthors(@Param("authors") List<Author> authors);
}
