package com.bookstore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
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

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "author", orphanRemoval = true)
    @Where(clause = "price <= 20")
    private List<Book> cheapBooks = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "author", orphanRemoval = true)
    @Where(clause = "price > 20")
    private List<Book> restOfBooks = new ArrayList<>();

    public List<Book> getBooks() {
        return books;
    }

    public List<Book> getCheapBooks() {
        return cheapBooks;
    }

    public List<Book> getRestOfBooks() {
        return restOfBooks;
    }

    @Override
    public String toString() {
        return "Author{" +
                "age=" + age +
                ", genre='" + genre + '\'' +
                ", name='" + name + '\'' +
                ", id=" + id +
                '}';
    }
}
