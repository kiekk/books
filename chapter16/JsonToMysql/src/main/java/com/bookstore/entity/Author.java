package com.bookstore.entity;

import com.bookstore.obj.Book;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Book book;

    public static Author createAuthor(String name, String genre, int age, Book book) {
        Author author = new Author();
        author.name = name;
        author.genre = genre;
        author.age = age;
        author.book = book;
        return author;
    }
}
