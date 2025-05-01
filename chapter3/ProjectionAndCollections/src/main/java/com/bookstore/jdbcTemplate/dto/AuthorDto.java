package com.bookstore.jdbcTemplate.dto;

import java.util.ArrayList;
import java.util.List;

public class AuthorDto {
    private Long authorId;
    private String name;
    private String genre;

    private List<BookDto> books = new ArrayList<>();

    public static AuthorDto createAuthorDto(Long authorId, String name, String genre) {
        AuthorDto authorDto = new AuthorDto();
        authorDto.authorId = authorId;
        authorDto.name = name;
        authorDto.genre = genre;
        authorDto.books = new ArrayList<>();
        return authorDto;
    }

    public void addBook(BookDto bookDto) {
        books.add(bookDto);
    }

    public Long getId() {
        return authorId;
    }

    public String getName() {
        return name;
    }

    public String getGenre() {
        return genre;
    }

    public List<BookDto> getBooks() {
        return books;
    }
}
