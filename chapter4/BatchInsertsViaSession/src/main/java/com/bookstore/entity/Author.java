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
}
