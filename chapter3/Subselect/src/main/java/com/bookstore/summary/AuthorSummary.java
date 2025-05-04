package com.bookstore.summary;

import com.bookstore.entity.Book;
import javax.persistence.Id;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.util.HashSet;
import java.util.Set;

@Entity
@Subselect(
        "SELECT a.id AS id, a.name AS name, a.genre AS genre FROM Author a")
@Synchronize({"author", "book"})
@Immutable
public class AuthorSummary {
    @Id
    private Long id;

    private String name;
    private String genre;

    @OneToMany(mappedBy = "author")
    private Set<Book> books = new HashSet<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGenre() {
        return genre;
    }

    public Set<Book> getBooks() {
        return books;
    }
}
