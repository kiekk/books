package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.obj.Book;
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
    public void newAuthor() {
        Book book = Book.createBook("001-JN", "A History of Ancient Prague", 45);

        Author author = Author.createAuthor("Joana Nimar", "History", 34, book);

        authorRepository.save(author);
    }

    @Transactional(readOnly = true)
    public void byName() {
        Author author = authorRepository.findByName("Joana Nimar");

        System.out.println(author);
    }

    @Transactional(readOnly = true)
    public void byNameIsbn() {
        Author author = authorRepository.findByBookIsbn("001-JN");

        System.out.println(author);
    }

    @Transactional(readOnly = true)
    public void byBookIsbnNativeQuery() {
        Author author = authorRepository.findByBookIsbnNativeQuery("001-JN");

        System.out.println(author);
    }
}
