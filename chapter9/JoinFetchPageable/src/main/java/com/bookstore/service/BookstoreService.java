package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional(readOnly = true)
    public Page<Author> fetchWithBooksByGenreCQ() {
        return authorRepository.fetchWithBooksByGenreCQ("Anthology",
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name")));
    }

    @Transactional(readOnly = true)
    public Page<Author> fetchWithBooksByGenreEG() {
        return authorRepository.fetchWithBooksByGenreEG("Anthology",
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name")));
    }
}
