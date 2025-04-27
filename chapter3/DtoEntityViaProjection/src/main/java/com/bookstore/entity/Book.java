package com.bookstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Book {
    @Id
    private Long id;

    private String title;
    private String isbn;
    private String genre;
}
