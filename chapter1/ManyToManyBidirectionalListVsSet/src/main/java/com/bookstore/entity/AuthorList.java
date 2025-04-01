package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Entity
public class AuthorList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "author_book_list",
            joinColumns = @JoinColumn(name = "author_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private List<BookList> books = new ArrayList<>();

    public static AuthorList createAuthor(String name, String genre, int age) {
        AuthorList author = new AuthorList();
        author.name = name;
        author.genre = genre;
        author.age = age;
        return author;
    }

    public void addBook(BookList book) {
        this.books.add(book);
        book.addAuthor(this);
    }

    public void removeBook(BookList book) {
        this.books.remove(book);
        book.removeAuthor(this);
    }

    public void removeBooks() {
        Iterator<BookList> iterator = this.books.iterator();

        while (iterator.hasNext()) {
            BookList book = iterator.next();

            book.removeAuthor(this);
            iterator.remove();
        }
    }
}
