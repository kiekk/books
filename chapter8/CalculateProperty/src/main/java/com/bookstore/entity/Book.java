package com.bookstore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    private String title;
    private String isbn;
    private double price;
    @Transient
    private double discounted;

    /*
    @Transient
    public double getDiscounted() {
        System.out.println("Call getDiscounted() method");
        return this.price - this.price * 0.25;
    }
    */

    @PostLoad
    private void postLoad() {
        System.out.println("Call postLoad() method");
        this.discounted = this.price - this.price * 0.25;
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
