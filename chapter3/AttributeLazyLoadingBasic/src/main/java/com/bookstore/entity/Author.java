package com.bookstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

@Entity
public class Author {
    @Id
    private Long id;

    @Lob
    private byte[] avatar;

    private int age;
    private String name;
    private String genre;


}
