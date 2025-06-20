package com.bookstore.service;

import com.bookstore.dto.BookstoreDto;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public List<BookstoreDto> fetchAuthorNameBookTitleWithPrice(int price) {
        return authorRepository.fetchAuthorNameBookTitleWithPrice(price);
    }
}
