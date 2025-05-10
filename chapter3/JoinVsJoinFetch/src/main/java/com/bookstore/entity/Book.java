package com.bookstore.entity;

import jakarta.persistence.*;

@Entity
public class Book {
    @Id
    private Long id;

    private String title;
    private String isbn;
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;
}
