package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public void persistAuthor() {
        Author author = Author.createAuthor("Alicia Weys", 43, "Horror", true);

        authorRepository.save(author);
    }

    public void fetchAuthor() {
        Author author = authorRepository.findByName("Alicia Weys");
        System.out.println(author);
    }
}
