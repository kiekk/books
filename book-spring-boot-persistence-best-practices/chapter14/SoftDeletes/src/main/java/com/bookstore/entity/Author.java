package com.bookstore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@SQLDelete(sql = """
            UPDATE author
            SET deleted = true
            WHERE id = ?
        """)
@SQLRestriction("deleted = false")
public class Author extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @OneToMany(cascade = CascadeType.ALL,
            mappedBy = "author", orphanRemoval = true)
    private List<Book> books = new ArrayList<>();

    public String getName() {
        return name;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void removeBook(Book book) {
        book.removeAuthor();
        this.books.remove(book);
    }

    @PreRemove
    private void bookRemove() {
        deleted = true;
    }
}
