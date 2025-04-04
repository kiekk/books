package com.bookstore.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@NamedEntityGraph(
        name = "author-books-publisher-graph",
        attributeNodes = {
                @NamedAttributeNode(value = "books", subgraph = "publisher-subgraph")
        },
        subgraphs = {
                @NamedSubgraph(
                        name = "publisher-subgraph",
                        attributeNodes = {
                                @NamedAttributeNode("publisher")
                        }
                )
        }
)
public class Author {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String genre;
    private int age;

    @OneToMany(cascade = CascadeType.ALL,
            mappedBy = "author", orphanRemoval = true)
    private List<Book> books = new ArrayList<>();


}
