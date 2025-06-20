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

    @Transactional
    public void newBookOfAuthor() {
        Author author = authorRepository.findById(1L).orElseThrow();

        Book book = Book.createBook("A History of Ancient Prague", "001-JN");

        book.addAuthor(author);

        bookRepository.save(book);
    }

    @Transactional(readOnly = true)
    public Book fetchBookByAuthorId() {
        Author author = authorRepository.findById(1L).orElseThrow();

        return bookRepository.findById(author.getId()).orElseThrow();
    }

    @Transactional(readOnly = true)
    public Book fetchBookByAuthor() {
        Author author = authorRepository.findById(1L).orElseThrow();
        return bookRepository.fetchBookByAuthor(author);
    }
}
