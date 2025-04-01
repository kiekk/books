package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
public class BookSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToMany(mappedBy = "books")
    @OrderBy("name ASC") // HashSet을 사용하더라도 내부적으로는 LinkedHashSet을 사용하기 때문에 LinkedHashSet을 할당하는 것이 더 좋습니다.
    private Set<AuthorSet> authors = new LinkedHashSet<>();

    public static BookSet createBook(String title, String isbn) {
        BookSet book = new BookSet();
        book.title = title;
        book.isbn = isbn;
        return book;
    }

    public void addAuthor(AuthorSet author) {
        this.authors.add(author);
    }

    public void removeAuthor(AuthorSet author) {
        this.authors.remove(author);
    }

    public Set<AuthorSet> getAuthors() {
        return authors;
    }
}
