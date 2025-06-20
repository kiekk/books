package com.bookstore.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Book {
    @Id
    private Long id;

    private String title;
    private String isbn;
    private String author;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "book", orphanRemoval = true)
    private List<BookReview> reviews = new ArrayList<>();

    public static Book createBook(Long id, String title, String isbn, String author) {
        Book book = new Book();
        book.id = id;
        book.title = title;
        book.isbn = isbn;
        book.author = author;
        return book;
    }

    public void addReview(BookReview review) {
        this.reviews.add(review);
        review.addBook(this);
    }

    public void removeReview(BookReview review) {
        this.reviews.remove(review);
        review.removeBook();
    }
}
