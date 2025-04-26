package com.bookstore.service;

import com.bookstore.dto.AuthorDto;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> fetchAll() {
        return authorRepository.fetchAll();
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> fetchAgeNameGenre() {
        return authorRepository.fetchAgeNameGenre();
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> fetchNameEmail() {
        return authorRepository.fetchNameEmail();
    }
}
