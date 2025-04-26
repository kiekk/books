package com.bookstore.entity;

import jakarta.persistence.*;

@NamedQuery(
        name = "Author.fetchName",
        query = "SELECT a.name FROM Author a"
)
@NamedQuery(
        name = "Author.fetchNameAndAge",
        query = "SELECT a.age AS age, a.name AS name FROM Author a"
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
