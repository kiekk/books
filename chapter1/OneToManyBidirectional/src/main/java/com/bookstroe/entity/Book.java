package com.bookstroe.entity;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    // @~ToOne은 default FetchType 이 EAGER로 설정되어 있기 때문에 LAZY로 설정한다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    public static Book createBook(String title, String isbn) {
        Book book = new Book();
        book.title = title;
        book.isbn = isbn;
        return book;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIsbn() {
        return isbn;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(id, book.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Book{" +
                "isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", id=" + id +
                '}';
    }
}
