package com.bookstore.entity;

import com.bookstore.converter.BooleanConverter;
import jakarta.persistence.*;

@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int age;
    private String name;
    private String genre;

    @Convert(converter = BooleanConverter.class)
    private Boolean bestSelling;

    public static Author createAuthor(String name, int age, String genre, Boolean bestSelling) {
        Author author = new Author();
        author.name = name;
        author.age = age;
        author.genre = genre;
        author.bestSelling = bestSelling;

        return author;
    }

    @Override
    public String toString() {
        return "Author{" +
                "id=" + id +
                ", age=" + age +
                ", name='" + name + '\'' +
                ", genre='" + genre + '\'' +
                '}';
    }
}
