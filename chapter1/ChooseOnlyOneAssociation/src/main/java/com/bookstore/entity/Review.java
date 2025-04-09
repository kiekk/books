package com.bookstore.entity;

import com.bookstore.validation.JustOneOfMany;
import jakarta.persistence.*;

@Entity
@JustOneOfMany
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    private Magazine magazine;

    public static Review createReview(String content) {
        Review review = new Review();
        review.content = content;
        return review;
    }

    public void addBook(Book book) {
        this.book = book;
    }

    public void addArticle(Article article) {
        this.article = article;
    }

    public void addMagazine(Magazine magazine) {
        this.magazine = magazine;
    }

    public Book getBook() {
        return book;
    }

    public Article getArticle() {
        return article;
    }

    public Magazine getMagazine() {
        return magazine;
    }

    @Override
    public String toString() {
        return "Review{" +
                "id=" + id +
                ", content='" + content + '\'' +
                '}';
    }
}
