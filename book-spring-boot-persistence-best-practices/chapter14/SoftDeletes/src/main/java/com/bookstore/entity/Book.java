package com.bookstore.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SQLDelete(sql = """
            UPDATE book 
            SET deleted = true 
            WHERE id = ? 
        """)
@SQLRestriction("deleted = false")
public class Book extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String isbn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author author;

    public String getTitle() {
        return title;
    }

    public void removeAuthor() {
        this.author = null;
    }

    @PreRemove
    private void authorRemove() {
        deleted = true;
    }
}
