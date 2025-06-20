package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import com.bookstore.repository.AuthorRepository;
import com.bookstore.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public BookstoreService(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public Page<Author> fetchWithBooksByGenreCQ() {
        return authorRepository.fetchWithBooksByGenreCQ("Anthology",
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name")));
    }

    @Transactional(readOnly = true)
    public Page<Author> fetchWithBooksByGenreEG() {
        return authorRepository.fetchWithBooksByGenreEG("Anthology",
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name")));
    }

    @Transactional(readOnly = true)
    public Page<Book> fetchWithAuthorsByIsbnCQ() {
        return bookRepository.fetchWithAuthorsByIsbnCQ("001-",
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "title")));
    }

    @Transactional(readOnly = true)
    public Page<Book> fetchWithAuthorsByIsbnEG() {
        return bookRepository.fetchWithAuthorsByIsbnEG("001-",
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "title")));
    }
}
