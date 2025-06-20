package com.bookstore.dto;

import org.springframework.beans.factory.annotation.Value;

public interface VirtualBookDto {
    String getTitle(); // book title

    @Value("#{@authorMapper.buildAuthorDto(target.name, target.genre)}")
    AuthorClassDto getAuthor();
}
