package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    // INNER JOIN
    @Transactional(readOnly = true)
    public void fetchAuthorsBooksByPriceInnerJoin() {
        List<Author> authors = authorRepository.fetchAuthorsBooksByPriceInnerJoin(40);

        authors.forEach((e) -> System.out.println("Author name: " + e.getName()
                + ", books: " + e.getBooks())); // causes extra SELECTs and the result is not ok
    }

    // JOIN FETCH
    public void fetchAuthorsBooksByPriceJoinFetch() {
        List<Author> authors = authorRepository.fetchAuthorsBooksByPriceJoinFetch(40);

        authors.forEach((e) -> System.out.println("Author name: " + e.getName()
                + ", books: " + e.getBooks())); // does not cause extra SELECTs and the result is ok
    }
}
