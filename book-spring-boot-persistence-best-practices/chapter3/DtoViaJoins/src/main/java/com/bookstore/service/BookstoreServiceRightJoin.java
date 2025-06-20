package com.bookstore.service;

import com.bookstore.dto.AuthorNameBookTitle;
import com.bookstore.repository.AuthorRepositoryRightJoin;
import com.bookstore.repository.BookRepositoryRightJoin;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookstoreServiceRightJoin {

    private final AuthorRepositoryRightJoin authorRepositoryRightJoin;
    private final BookRepositoryRightJoin bookRepositoryRightJoin;

    public BookstoreServiceRightJoin(AuthorRepositoryRightJoin authorRepositoryRightJoin, BookRepositoryRightJoin bookRepositoryRightJoin) {
        this.authorRepositoryRightJoin = authorRepositoryRightJoin;
        this.bookRepositoryRightJoin = bookRepositoryRightJoin;
    }

    public List<AuthorNameBookTitle> fetchBooksAndAuthorsJpql() {
        return bookRepositoryRightJoin.findBooksAndAuthorsJpql();
    }

    public List<AuthorNameBookTitle> fetchBooksAndAuthorsSql() {
        return bookRepositoryRightJoin.findBooksAndAuthorsSql();
    }

    public List<AuthorNameBookTitle> fetchAuthorsAndBooksJpql() {
        return authorRepositoryRightJoin.findAuthorsAndBooksJpql();
    }

    public List<AuthorNameBookTitle> fetchAuthorsAndBooksSql() {
        return authorRepositoryRightJoin.findAuthorsAndBooksSql();
    }
}
