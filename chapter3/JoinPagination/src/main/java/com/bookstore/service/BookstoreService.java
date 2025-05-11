package com.bookstore.service;

import com.bookstore.dto.AuthorBookDto;
import com.bookstore.repository.AuthorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public Page<AuthorBookDto> fetchPageOfAuthorsWithBooksDtoByGenre(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<AuthorBookDto> pageOfAuthors = authorRepository.fetchPageOfDto("Anthology", pageable);
        return pageOfAuthors;
    }
}
