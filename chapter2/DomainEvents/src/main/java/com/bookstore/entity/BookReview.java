package com.bookstore.entity;

import jakarta.persistence.*;

@Entity
public class BookReview {
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

    public void addBook(Book book) {
        this.book = book;
    }

    public void removeBook() {
        this.book = null;
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
