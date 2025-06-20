package com.bookstore.service;

import com.bookstore.dao.AuthorDao;
import com.bookstore.dto.AuthorDtoNoSetters;
import com.bookstore.dto.AuthorDtoWithSetters;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookstoreService {

    private final AuthorDao authorDao;

    public BookstoreService(AuthorDao authorDao) {
        this.authorDao = authorDao;
    }

    public List<AuthorDtoNoSetters> fetchAuthorsNoSetters() {
        return authorDao.fetchAuthorsNoSetters();
    }

    public List<AuthorDtoWithSetters> fetchAuthorsWithSetters() {
        return authorDao.fetchAuthorsWithSetters();
    }
}
