package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Entity
public class AuthorSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "author_book_set",
            joinColumns = @JoinColumn(name = "author_id"),
            inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<BookSet> books = new HashSet<>();

    public static AuthorSet createAuthor(String name, String genre, int age) {
        AuthorSet author = new AuthorSet();
        author.name = name;
        author.genre = genre;
        author.age = age;
        return author;
    }

    public void addBook(BookSet book) {
        this.books.add(book);
        book.addAuthor(this);
    }

    public void removeBook(BookSet book) {
        this.books.remove(book);
        book.removeAuthor(this);
    }

    public void removeBooks() {
        Iterator<BookSet> iterator = this.books.iterator();

        while (iterator.hasNext()) {
            BookSet book = iterator.next();

            book.removeAuthor(this);
            iterator.remove();
        }
    }
}
