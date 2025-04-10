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

    public Book setId(Long id) {
        this.id = id;
        return this;
    }

    public Book setTitle(String title) {
        this.title = title;
        return this;
    }

    public Book setIsbn(String isbn) {
        this.isbn = isbn;
        return this;
    }

    public Book setAuthor(Author author) {
        this.author = author;
        return this;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", isbn='" + isbn + '\'' +
                '}';
    }
}
