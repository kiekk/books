package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@NamedEntityGraph(
        name = "author-books-graph",
        attributeNodes = {
                @NamedAttributeNode("name"),
                @NamedAttributeNode("books")
        }
)
@Entity
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Basic(fetch = FetchType.LAZY)
    private String genre;

    @Basic(fetch = FetchType.LAZY)
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
}
