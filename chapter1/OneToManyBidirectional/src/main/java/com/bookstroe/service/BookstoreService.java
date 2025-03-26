package com.bookstroe.service;

import com.bookstroe.entity.Author;
import com.bookstroe.entity.Book;
import com.bookstroe.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public void insertAuthorWithBooks() {
        Author author = Author.createAuthor("Alicia Tom", "Anthology", 38);

        Book book = Book.createBook("001-AT", "The book of swords");

        author.addBook(book); // use addBook() helper

        authorRepository.save(author);
    }

    @Transactional
    public void deleteBookOfAuthor() {
        Author author = authorRepository.fetchByName("Alicia Tom");
        Book book = author.getBooks().getFirst();

        author.removeBook(book); // use removeBook() helper
    }

    public void insertOtherAuthorWithBooks() {
        Author author = Author.createAuthor("Joana Nimar", "History", 34);

        Book book01 = Book.createBook("001-JN", "A History of Ancient Prague");
        Book book02 = Book.createBook("002-JN", "A People's History");
        Book book03 = Book.createBook("003-JN", "A People's History");

        author.addBooks(book01, book02, book03); // use addBooks() helper

        authorRepository.save(author);
    }

    @Transactional
    public void deleteAllBooksOfAuthor() {
        Author author = authorRepository.fetchByName("Joana Nimar");
        author.removeBooks(); // use removeBooks() helper
    }
}
