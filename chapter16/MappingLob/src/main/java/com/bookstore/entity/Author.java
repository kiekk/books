package com.bookstore.entity;

import jakarta.persistence.*;

@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int age;
    private String name;
    private String genre;

    @Lob
    private byte[] avatar;

    @Lob
    private String biography;

    public static Author createAuthor(String name, int age, String genre) {
        Author author = new Author();
        author.name = name;
        author.age = age;
        author.genre = genre;
        return author;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public String getBiography() {
        return biography;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }
}
