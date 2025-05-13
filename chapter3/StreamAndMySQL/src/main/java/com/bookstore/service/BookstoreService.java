package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.repository.AuthorRepository;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Service
public class BookstoreService {

    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public void populateDatabase() {
        List<Author> authors = new ArrayList<>();

        for (long i = 0; i < 1000; i++) {
            Author author = Author.createAuthor("Author_" + i, "Genre_" + i, (int) (Math.random() * 100));

            authors.add(author);
        }

        authorRepository.saveAll(authors);
    }

    @Transactional(readOnly = true)
    public void streamDatabase() {
        long startTime = System.nanoTime();
        try (Stream<Author> authorStream = authorRepository.streamAll()) {
            authorStream.forEach(System.out::println);
        }
        System.out.println("Total time: "
                + TimeUnit.MILLISECONDS.convert((System.nanoTime() - startTime), TimeUnit.NANOSECONDS) + " ms");
    }

    @Transactional(readOnly = true)
    public void streamDatabase2() {
        long startTime = System.nanoTime();
        try (Stream<Author> authorStream = authorRepository.streamAll2()) {
            authorStream.forEach(System.out::println);
        }
        System.out.println("Total time: "
                + TimeUnit.MILLISECONDS.convert((System.nanoTime() - startTime), TimeUnit.NANOSECONDS) + " ms");
    }

    @Transactional(readOnly = true)
    public void fetchAuthorsAsStreamable() {
        Streamable<Author> authors = authorRepository.findByGenre("Anthology");

        authors.forEach(System.out::println);
    }

    @Transactional(readOnly = true)
    public void fetchAuthorsByGenreConcatAge() {
        Streamable<Author> authors = authorRepository.findByGenre("Anthology")
                .and(authorRepository.findByAgeGreaterThan(40));

        authors.forEach(System.out::println);
    }
}
