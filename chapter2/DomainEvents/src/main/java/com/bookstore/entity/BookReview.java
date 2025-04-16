package com.bookstore.entity;

import com.bookstore.event.CheckReviewEvent;
import jakarta.persistence.*;
import org.springframework.data.domain.AbstractAggregateRoot;

@Entity
public class BookReview extends AbstractAggregateRoot<BookReview> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;
    private String email;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    public static BookReview createBookReview(String content, String email, ReviewStatus status) {
        BookReview bookReview = new BookReview();
        bookReview.content = content;
        bookReview.email = email;
        bookReview.status = status;
        return bookReview;
    }

    public void registerReviewEvent() {
        registerEvent(new CheckReviewEvent(this));
    }

    public void addBook(Book book) {
        this.book = book;
    }

    public void removeBook() {
        this.book = null;
    }

    public void changeStatus(ReviewStatus reviewStatus) {
        this.status = reviewStatus;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return "BookReview{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                '}';
    }

}
