package com.bookstore.entity;

import jakarta.persistence.*;

import java.sql.Blob;
import java.sql.Clob;

@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int age;
    private String name;
    private String genre;

    @Lob
    private Blob avatar;

    @Lob
    private Clob biography;

    public static Author createAuthor(String name, int age, String genre) {
        Author author = new Author();
        author.name = name;
        author.age = age;
        author.genre = genre;
        return author;
    }

    public void setAvatar(Blob avatar) {
        this.avatar = avatar;
    }

    public void setBiography(Clob biography) {
        this.biography = biography;
    }

    public Blob getAvatar() {
        return avatar;
    }

    public Clob getBiography() {
        return biography;
    }
}
