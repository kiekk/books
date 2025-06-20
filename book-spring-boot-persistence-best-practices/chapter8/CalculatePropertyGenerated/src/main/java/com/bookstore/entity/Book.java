package com.bookstore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    private String title;
    private String isbn;
    private double price;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(insertable = false, updatable = false /*, columnDefinition = "double AS (price - price * 0.25)"*/)
    private double discounted;

    public static Book createBook(String title, String isbn, double price) {
        Book book = new Book();
        book.title = title;
        book.isbn = isbn;
        book.price = price;
        return book;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDiscounted() {
        return discounted;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", isbn='" + isbn + '\'' +
                ", price=" + price +
                ", discounted=" + discounted +
                '}';
    }
}
