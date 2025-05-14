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
}
