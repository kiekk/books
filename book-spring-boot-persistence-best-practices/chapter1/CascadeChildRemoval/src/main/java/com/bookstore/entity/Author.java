package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "author", orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    public void addBook(Book book) {
        this.books.add(book);
        book.addAuthor(this);
    }

    public void removeBook(Book book) {
        book.removeAuthor();
        this.books.remove(book);
    }

    public void removeBooks() {
        Iterator<Book> iterator = this.books.iterator();

        while (iterator.hasNext()) {
            Book book = iterator.next();

            book.removeAuthor();
            iterator.remove();
        }
    }

    public Long getId() {
        return id;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void changeGenre(String genre) {
        this.genre = genre;
    }
}
