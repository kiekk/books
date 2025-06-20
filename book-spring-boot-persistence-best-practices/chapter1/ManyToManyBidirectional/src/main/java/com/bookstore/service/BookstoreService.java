package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional
    public void insertAuthorsWithBooks() {
        Author alicia = Author.createAuthor("Alicia Tom", "Anthology", 38);
        Author mark = Author.createAuthor("Mark Janel", "Anthology", 38);

        Book bookOfSwords = Book.createBook("The book of swords", "001-AT-MJ");
        Book oneDay = Book.createBook("One Day", "002-AT-MJ");

        alicia.addBook(bookOfSwords); // use addBook() helper
        mark.addBook(oneDay);

        authorRepository.save(alicia);
        authorRepository.save(mark);
    }
}
