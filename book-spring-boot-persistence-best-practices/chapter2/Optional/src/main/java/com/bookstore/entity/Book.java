package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.Optional;

@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    public void addAuthor(Author author) {
        this.author = author;
    }

    public void removeAuthor() {
        this.author = null;
    }

    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    public Optional<String> getIsbn() {
        return Optional.ofNullable(isbn);
    }

    public Optional<Author> getAuthor() {
        return Optional.ofNullable(author);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Book)) {
            return false;
        }

        return id != null && id.equals(((Book) obj).id);
    }

    @Override
    public int hashCode() {
        return 2021;
    }

    @Override
    public String toString() {
        return "Book{" + "id=" + id + ", title=" + title + ", isbn=" + isbn + '}';
    }

}
