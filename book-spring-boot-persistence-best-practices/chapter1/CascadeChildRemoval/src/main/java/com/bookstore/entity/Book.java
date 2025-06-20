package com.bookstore.entity;

import jakarta.persistence.*;

@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    public void addAuthor(Author author) {
        this.author = author;
    }

    public void removeAuthor() {
        this.author = null;
    }
}
