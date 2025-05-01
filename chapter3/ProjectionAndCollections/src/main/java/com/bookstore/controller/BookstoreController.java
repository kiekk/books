package com.bookstore.controller;

import com.bookstore.dto.AuthorDto;
import com.bookstore.service.BookstoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookstoreController {

    private final BookstoreService bookstoreService;

    public BookstoreController(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    @GetMapping("/authorsAndbooks/1")
    public List<AuthorDto> fetchAuthorsWithBooksQueryBuilderMechanism() {
        return bookstoreService.fetchAuthorsWithBooksQueryBuilderMechanism();
    }

    @GetMapping("/authorsAndbooks/2")
    public List<AuthorDto> fetchAuthorsWithBooksViaQuery() {
        return bookstoreService.fetchAuthorsWithBooksViaQuery();
    }

}
