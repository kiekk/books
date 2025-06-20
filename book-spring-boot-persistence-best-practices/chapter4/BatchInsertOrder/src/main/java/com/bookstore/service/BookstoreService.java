package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
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

    public void batchAuthorsAndBooks() {
        List<Author> authors = new ArrayList<>();

        long pk = 0;
        for (int i = 0; i < 40; i++) {
            Author author = Author.createAuthor((long) i + 1, "Name_" + i, "Genre_" + i, 18 + i);

            for (int j = 0; j < 5; j++) {
                Book book = Book.createBook(++pk, "Title: " + j, "Isbn: " + j);
                author.addBook(book);
            }

            authors.add(author);
        }

        authorRepository.saveInBatch(authors);
    }
}
