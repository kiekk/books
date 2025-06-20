package com.bookstore.repository;

import com.bookstore.dto.BookTitleAndFormatType;
import com.bookstore.entity.Format;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FormatRepositoryCrossJoin extends JpaRepository<Format, Long> {
    @Query(value = "SELECT b.title AS title, f.formatType AS formatType "
            + "FROM Format f, Book b")
    List<BookTitleAndFormatType> findFormatsAndBooksJpql();

    @Query(value = "SELECT b.title AS title, f.format_type AS formatType "
            + "FROM format f CROSS JOIN book b",
            nativeQuery = true)
    List<BookTitleAndFormatType> findFormatsAndBooksSql();
}
