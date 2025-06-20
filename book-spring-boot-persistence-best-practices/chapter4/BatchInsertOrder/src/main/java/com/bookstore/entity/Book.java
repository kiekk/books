package com.bookstore.entity;

import jakarta.persistence.*;

@Entity
public class Book {
    @Id
    private Long id;

    private String title;
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    public static Book createBook(Long id, String title, String isbn) {
        Book book = new Book();
        book.id = id;
        book.title = title;
        book.isbn = isbn;
        return book;
    }

    public void addAuthor(Author author) {
        this.author = author;
    }
}
