package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.reverseOrder;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public void insertAuthorWithBooks() {
        Author author = Author.createAuthor("Joana Nimar", "History", 34);
        Book book01 = Book.createBook("001-JN", "A History of Ancient Prague");
        Book book02 = Book.createBook("002-JN", "A People's History");
        Book book03 = Book.createBook("003-JN", "World History");

        author.addBooks(book01, book02, book03); // use addBooks() helper
        authorRepository.save(author);
    }

    @Transactional
    public void insertNewBook() {
        Author author = authorRepository.fetchByName("Joana Nimar");
        Book book = Book.createBook("004-JN", "History Details");

        author.addBook(book); // use addBook() helper
        authorRepository.save(author);
    }

    @Transactional
    public void deleteLastBook() {
        Author author = authorRepository.fetchByName("Joana Nimar");
        Set<Book> books = author.getBooks();
        Collections.reverse(Arrays.asList(books.toArray()));
        author.removeBook(books.stream().findFirst().get());
    }

    @Transactional
    public void deleteFirstBook() {
        Author author = authorRepository.fetchByName("Joana Nimar");
        Set<Book> books = author.getBooks();
        author.removeBook(books.stream().findFirst().get());
    }
}
