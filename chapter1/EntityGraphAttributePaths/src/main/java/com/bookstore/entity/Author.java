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

    @OneToMany(cascade = CascadeType.ALL,
            mappedBy = "author", orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    public static Author createAuthor(String name, String genre, int age) {
        Author author = new Author();
        author.name = name;
        author.genre = genre;
        author.age = age;
        return author;
    }

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
}
