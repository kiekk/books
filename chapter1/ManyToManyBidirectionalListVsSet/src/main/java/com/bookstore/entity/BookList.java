package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class BookList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToMany(mappedBy = "books")
    private List<AuthorList> authors = new ArrayList<>();

    public static BookList createBook(String title, String isbn) {
        BookList book = new BookList();
        book.title = title;
        book.isbn = isbn;
        return book;
    }

    public void addAuthor(AuthorList author) {
        this.authors.add(author);
    }

    public void removeAuthor(AuthorList author) {
        this.authors.remove(author);
    }
}
