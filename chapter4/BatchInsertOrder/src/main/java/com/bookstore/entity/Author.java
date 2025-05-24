package com.bookstore.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Author {
    @Id
    private Long id;

    private String name;
    private String genre;
    private int age;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "author", orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    public static Author createAuthor(Long id, String name, String genre, int age) {
        Author author = new Author();
        author.id = id;
        author.name = name;
        author.genre = genre;
        author.age = age;
        return author;
    }

    public void addBook(Book book) {
        books.add(book);
        book.addAuthor(this);
    }
}
