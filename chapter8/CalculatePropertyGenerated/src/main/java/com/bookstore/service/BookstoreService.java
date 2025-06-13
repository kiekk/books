package com.bookstore.service;

import com.bookstore.entity.Book;
import com.bookstore.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {

    private final BookRepository bookRepository;

    public BookstoreService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional
    public void insertBook() {
        Book book = Book.createBook("Ancient History", "001-AH", 13.99);

        bookRepository.save(book);

        System.out.println("Discounted price after insert: " + book.getDiscounted());
    }

    @Transactional
    public void updateBook() {
        Book book = bookRepository.findByTitle("Ancient History");
        book.setPrice(9.99);

        bookRepository.flush();

        System.out.println("Discounted price after update: " + book.getDiscounted());
    }
}
