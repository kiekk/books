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

    @OneToMany(cascade = CascadeType.PERSIST, mappedBy = "author", orphanRemoval = false)
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

    @Override
    public String toString() {
        return "Author{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", genre='" + genre + '\'' +
                ", age=" + age +
                '}';
    }
}
