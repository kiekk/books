package com.bookstore.entity;

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

    // collection을 Set으로 할 경우 equals와 hashCode를 재정의해야 한다.
    // 그렇지 않고 id만을 사용할 경우 모두 같은 객체라고 판단하여 저장이 되지 않는다.
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(id, book.id) && Objects.equals(title, book.title) && Objects.equals(isbn, book.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, isbn);
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
