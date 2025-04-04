package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Publisher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String company;

    @OneToMany(cascade = CascadeType.ALL,
            mappedBy = "publisher", orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    public List<Book> getBooks() {
        return books;
    }

    @Override
    public String toString() {
        return "Publisher{" +
                "id=" + id +
                ", company='" + company + '\'' +
                '}';
    }
}
