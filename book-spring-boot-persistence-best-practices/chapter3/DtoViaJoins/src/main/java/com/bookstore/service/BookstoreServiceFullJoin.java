package com.bookstore.service;

import com.bookstore.dto.AuthorNameBookTitle;
import com.bookstore.repository.AuthorRepositoryFullJoin;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookstoreServiceFullJoin {

    private final AuthorRepositoryFullJoin authorRepositoryFullJoin;

    public BookstoreServiceFullJoin(AuthorRepositoryFullJoin authorRepositoryFullJoin) {
        this.authorRepositoryFullJoin = authorRepositoryFullJoin;
    }

    public List<AuthorNameBookTitle> fetchAuthorsAndBooksSql() {
        return authorRepositoryFullJoin.findAuthorsAndBooksSql();
    }
}
