package com.bookstore.dto;

import java.util.ArrayList;
import java.util.List;

public class AuthorDto {
    private Long authorId;
    private String name;
    private int age;

    private List<BookDto> books = new ArrayList<>();

    public static AuthorDto createAuthorDto(Long authorId, String name, int age) {
        AuthorDto authorDto = new AuthorDto();
        authorDto.authorId = authorId;
        authorDto.name = name;
        authorDto.age = age;
        authorDto.books = new ArrayList<>();
        return authorDto;
    }

    public Long getId() {
        return authorId;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public List<BookDto> getBooks() {
        return books;
    }

    public void addBook(BookDto book) {
        books.add(book);
    }

    @Override
    public String toString() {
        return "AuthorDto{" + "authorId=" + authorId + ", name=" + name + ", age=" + age + '}';
    }
}
