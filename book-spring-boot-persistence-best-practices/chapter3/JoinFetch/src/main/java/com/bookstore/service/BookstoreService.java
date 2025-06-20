package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import com.bookstore.repository.AuthorRepository;
import com.bookstore.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public BookstoreService(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public Author fetchAuthorWithBooksByName() {
        return authorRepository.fetchAuthorWithBooksByName("Joana Nimar");
    }

    @Transactional(readOnly = true)
    public Book fetchBookWithAuthorByIsbn() {
        return bookRepository.fetchBookWithAuthorByIsbn("002-JN");
    }
}
