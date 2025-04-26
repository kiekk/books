package com.bookstore.entity;

import jakarta.persistence.*;

@NamedNativeQuery(
        name = "Author.fetchName",
        query = "SELECT name FROM author"
)
@NamedNativeQuery(
        name = "Author.fetchNameAndAge",
        query = "SELECT age, name FROM author"
)
@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int age;
    private String name;
    private String genre;
}
