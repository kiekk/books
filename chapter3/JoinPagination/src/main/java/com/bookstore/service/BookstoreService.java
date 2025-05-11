package com.bookstore.service;

import com.bookstore.dto.AuthorBookDto;
import com.bookstore.repository.AuthorRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public Page<AuthorBookDto> fetchPageOfAuthorsWithBooksDtoByGenreNative(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));

        List<AuthorBookDto> listOfAuthors = authorRepository.fetchListOfDtoNative("Anthology", pageable);
        Page<AuthorBookDto> pageOfAuthors
                = new PageImpl(listOfAuthors, pageable, listOfAuthors.isEmpty() ? 0 : listOfAuthors.getFirst().getTotal());

        return pageOfAuthors;
    }

    public Slice<AuthorBookDto> fetchSliceOfAuthorsWithBooksDtoByGenre(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Slice<AuthorBookDto> sliceOfAuthors = authorRepository.fetchSliceOfDto("Anthology", pageable);

        return sliceOfAuthors;
    }
}
