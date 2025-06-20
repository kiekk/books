package com.bookstore.service;

import com.bookstore.dto.AuthorNameBookTitle;
import com.bookstore.repository.AuthorRepositoryInnerJoin;
import com.bookstore.repository.BookRepositoryInnerJoin;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookstoreServiceInnerJoin {

    private final AuthorRepositoryInnerJoin authorRepositoryInnerJoin;
    private final BookRepositoryInnerJoin bookRepositoryInnerJoin;

    public BookstoreServiceInnerJoin(AuthorRepositoryInnerJoin authorRepositoryInnerJoin, BookRepositoryInnerJoin bookRepositoryInnerJoin) {
        this.authorRepositoryInnerJoin = authorRepositoryInnerJoin;
        this.bookRepositoryInnerJoin = bookRepositoryInnerJoin;
    }

    public List<AuthorNameBookTitle> fetchBooksAndAuthorsJpql() {
        return bookRepositoryInnerJoin.findBooksAndAuthorsJpql();
    }

    public List<AuthorNameBookTitle> fetchBooksAndAuthorsSql() {
        return bookRepositoryInnerJoin.findBooksAndAuthorsSql();
    }

    public List<AuthorNameBookTitle> fetchAuthorsAndBooksJpql() {
        return authorRepositoryInnerJoin.findAuthorsAndBooksJpql();
    }

    public List<AuthorNameBookTitle> fetchAuthorsAndBooksSql() {
        return authorRepositoryInnerJoin.findAuthorsAndBooksSql();
    }

    public List<AuthorNameBookTitle> findAuthorsAndBooksByGenreAndPriceJpql(String genre, int price) {
        return authorRepositoryInnerJoin.findAuthorsAndBooksByGenreAndPriceJpql(genre, price);
    }

    public List<AuthorNameBookTitle> findAuthorsAndBooksByGenreAndPriceSql(String genre, int price) {
        return authorRepositoryInnerJoin.findAuthorsAndBooksByGenreAndPriceSql(genre, price);
    }

    public List<AuthorNameBookTitle> findBooksAndAuthorsByGenreAndPriceJpql(String genre, int price) {
        return bookRepositoryInnerJoin.findBooksAndAuthorsByGenreAndPriceJpql(genre, price);
    }

    public List<AuthorNameBookTitle> findBooksAndAuthorsByGenreAndPriceSql(String genre, int price) {
        return bookRepositoryInnerJoin.findBooksAndAuthorsByGenreAndPriceSql(genre, price);
    }
}
