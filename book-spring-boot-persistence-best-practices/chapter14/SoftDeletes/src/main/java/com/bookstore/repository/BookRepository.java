package com.bookstore.repository;

import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query(value = "SELECT * FROM book", nativeQuery = true)
    List<Book> findAllIncludingDeleted();

    @Query(value = "SELECT * FROM book AS b WHERE b.deleted = true", nativeQuery = true)
    List<Book> findAllOnlyDeleted();

    @Query(value = "UPDATE Book b SET b.deleted = false WHERE b.author.id = ?1")
    @Modifying
    void restoreByAuthorId(Long id);

    @Query(value = "UPDATE Book b SET b.deleted = false WHERE b.id = ?1")
    @Modifying
    void restoreById(Long id);
}
