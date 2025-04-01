package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class BookSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToMany(mappedBy = "books")
    private Set<AuthorSet> authors = new HashSet<>();

    public static BookSet createBook(String title, String isbn) {
        BookSet book = new BookSet();
        book.title = title;
        book.isbn = isbn;
        return book;
    }

    public void addAuthor(AuthorSet author) {
        this.authors.add(author);
    }

    public void removeAuthor(AuthorSet author) {
        this.authors.remove(author);
    }
}
