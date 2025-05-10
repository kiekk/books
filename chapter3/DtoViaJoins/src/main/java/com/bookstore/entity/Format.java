package com.bookstore.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Format {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String formatType;

    public Long getId() {
        return id;
    }

    public String getFormatType() {
        return formatType;
    }

    @Override
    public String toString() {
        return "Format{" +
                "id=" + id +
                ", formatType='" + formatType + '\'' +
                '}';
    }
}
