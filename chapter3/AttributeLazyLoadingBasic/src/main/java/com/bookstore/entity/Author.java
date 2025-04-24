package com.bookstore.entity;

import jakarta.persistence.*;

@Entity
public class Author {
    @Id
    private Long id;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] avatar;

    private int age;
    private String name;
    private String genre;

    public static Author createAuthor(Long id, String name, int age, String genre, byte[] avatar) {
        Author author = new Author();
        author.id = id;
        author.name = name;
        author.age = age;
        author.genre = genre;
        author.avatar = avatar;
        return author;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    @Override
    public String toString() {
        return "Author{" +
                "id=" + id +
                ", age=" + age +
                ", genre='" + genre + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
