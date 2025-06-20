package com.bookstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Author {
    @Id
    private Long id;

    private String name;
    private String genre;
    private int age;

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setAge(int age) {
        this.age = age;
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
