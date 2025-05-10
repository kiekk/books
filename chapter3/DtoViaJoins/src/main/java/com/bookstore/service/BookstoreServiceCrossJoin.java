package com.bookstore.service;

import com.bookstore.dto.AuthorNameBookTitle;
import com.bookstore.dto.BookTitleAndFormatType;
import com.bookstore.repository.BookRepositoryCrossJoin;
import com.bookstore.repository.FormatRepositoryCrossJoin;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookstoreServiceCrossJoin {

    private final BookRepositoryCrossJoin bookRepositoryCrossJoin;
    private final FormatRepositoryCrossJoin formatRepositoryCrossJoin;

    public BookstoreServiceCrossJoin(FormatRepositoryCrossJoin formatRepositoryCrossJoin, BookRepositoryCrossJoin bookRepositoryCrossJoin) {
        this.formatRepositoryCrossJoin = formatRepositoryCrossJoin;
        this.bookRepositoryCrossJoin = bookRepositoryCrossJoin;
    }

    public List<BookTitleAndFormatType> fetchBooksAndFormatsJpql() {
        return bookRepositoryCrossJoin.findBooksAndFormatsJpql();
    }

    public List<BookTitleAndFormatType> fetchBooksAndFormatsSql() {
        return bookRepositoryCrossJoin.findBooksAndFormatsSql();
    }

    public List<BookTitleAndFormatType> fetchFormatsAndBooksJpql() {
        return formatRepositoryCrossJoin.findFormatsAndBooksJpql();
    }

    public List<BookTitleAndFormatType> fetchFormatsAndBooksSql() {
        return formatRepositoryCrossJoin.findFormatsAndBooksSql();
    }

    public List<AuthorNameBookTitle> fetchBooksAndAuthorsJpql() {
        return bookRepositoryCrossJoin.findBooksAndAuthorsJpql();
    }
}
