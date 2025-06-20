package com.bookstore.repository;

import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query(value = "SELECT b FROM Book b WHERE b.isbn "
            + "= function('concat_ws', '-', :code, :author)")
    Book fetchByIsbn(@Param("code") String code, @Param("author") String author);
}
