package com.bookstore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JoinFormula;

@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinFormula("(SELECT b.id FROM book b "
            + "WHERE b.price < price AND b.author_id = author_id "
            + "ORDER BY b.price DESC "
            + "LIMIT 1)")
    private Book nextBook;

    public int getPrice() {
        return price;
    }

    public Author getAuthor() {
        return author;
    }

    public Book getNextBook() {
        return nextBook;
    }

    @Override
    public String toString() {
        return "Book{" +
                "price=" + price +
                ", isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", id=" + id +
                '}';
    }
}
