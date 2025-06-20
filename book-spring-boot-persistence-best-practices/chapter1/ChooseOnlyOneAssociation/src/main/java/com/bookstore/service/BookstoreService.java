package com.bookstore.service;

import com.bookstore.entity.Review;
import com.bookstore.repository.ArticleRepository;
import com.bookstore.repository.BookRepository;
import com.bookstore.repository.MagazineRepository;
import com.bookstore.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {
    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final ArticleRepository articleRepository;
    private final MagazineRepository magazineRepository;

    public BookstoreService(ReviewRepository reviewRepository, BookRepository bookRepository,
                            ArticleRepository articleRepository, MagazineRepository magazineRepository) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.articleRepository = articleRepository;
        this.magazineRepository = magazineRepository;
    }

    @Transactional
    public void persistReviewOk() {
        Review review = Review.createReview("This is a book review ...");
        review.addBook(bookRepository.findById(1L).get());

        reviewRepository.save(review);
    }

    @Transactional
    public void persistReviewWrong() {
        Review review = Review.createReview("This is an article and magazine review ...");
        review.addMagazine(magazineRepository.findById(1L).get());
        review.addArticle(articleRepository.findById(1L).get());

        reviewRepository.save(review);
    }
}
