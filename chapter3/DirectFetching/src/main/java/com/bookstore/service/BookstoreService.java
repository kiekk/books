package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public void directFetching() {
        // direct fetching via Spring Data
        Optional<Author> resultSD = authorRepository.findById(1L);
        System.out.println("Direct fetching via Spring Data result: " + resultSD.get());
    }
}
