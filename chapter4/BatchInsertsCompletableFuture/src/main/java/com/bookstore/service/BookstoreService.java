package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public void batchAuthors() {
        List<Author> authors = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            Author author = Author.createAuthor((long) i + 1, "Name_" + i, "Genre_" + i, 18 + i);

            authors.add(author);
        }

        authorRepository.saveInBatch(authors);
    }
}
