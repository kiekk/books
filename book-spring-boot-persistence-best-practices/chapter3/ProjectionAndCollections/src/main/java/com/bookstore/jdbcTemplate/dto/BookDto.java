package com.bookstore.jdbcTemplate.dto;

public class BookDto {
    private Long bookId;
    private String title;

    public static BookDto createBookDto(Long bookId, String title) {
        BookDto bookDto = new BookDto();
        bookDto.bookId = bookId;
        bookDto.title = title;
        return bookDto;
    }

    public Long getId() {
        return bookId;
    }

    public String getTitle() {
        return title;
    }
}
