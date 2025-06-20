package com.bookstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class Author {
    @Id
    private Long id;

    private String name;
    private String genre;
    private int age;

    @Version
    private Short version;

    public static Author createAuthor(Long id, String name, String genre, int age) {
        Author author = new Author();
        author.id = id;
        author.name = name;
        author.genre = genre;
        author.age = age;
        return author;
    }
}
