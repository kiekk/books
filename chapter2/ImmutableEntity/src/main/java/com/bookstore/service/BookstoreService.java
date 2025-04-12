package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public void newAuthor() {
        Author author = Author.createAuthor(1L, "Joana Nimar", "History", 34);

        authorRepository.save(author);
    }

    public void fetchAuthor() {
        Author author = authorRepository.findById(1L).orElseThrow();
        System.out.println(author);
    }

    @Transactional
    public void updateAuthor() {
        Author author = authorRepository.findById(1L).orElseThrow();
        author.changeAge(45);
    }

    public void deleteAuthor() {
        authorRepository.deleteById(1L);
    }
}
