package com.bookstore.service;

import com.bookstore.entity.Book;
import com.bookstore.entity.BookReview;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.BookReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {
    private final static String RESPONSE
            = "We check your review and get back to you with an e-mail ASAP :)";

    private final BookRepository bookRepository;
    private final BookReviewRepository bookReviewRepository;

    public BookstoreService(BookReviewRepository bookReviewRepository, BookRepository bookRepository) {
        this.bookReviewRepository = bookReviewRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public void insertBook() {
        Book book = Book.createBook(1L, "001-LD", "Lucky Day", "Mark Janel");

        bookRepository.save(book);
    }

    @Transactional
    public String postReview(BookReview bookReview) {
        Book book = bookRepository.getReferenceById(1L);
        bookReview.addBook(book);

        // 이벤트 호출
        // save() 메서드 호출되고 트랜잭션 커밋된 후에 이벤트 발행
        bookReview.registerReviewEvent();

        bookReviewRepository.save(bookReview);

        return RESPONSE;
    }
}
