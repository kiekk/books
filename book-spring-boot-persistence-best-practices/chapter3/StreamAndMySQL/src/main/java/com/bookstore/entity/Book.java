package com.bookstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int price;
    private String title;

    public void setPrice(int price) {
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public int getPrice() {
        return price;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", price=" + price +
                ", title='" + title + '\'' +
                '}';
    }
}
