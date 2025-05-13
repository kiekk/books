package com.bookstore.service;

import com.bookstore.dto.AuthorName;
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

    // 필요한 것보다 더 많은 열을 가져오면 성능 저하가 발생되므로, 아래와 같은 방식으로는 사용하면 안된다.
    public void fetchAuthorsNames1() {
        Streamable<String> authors = authorRepository.findByGenre("Anthology")
                .map(Author::getName);

        authors.forEach(System.out::println);
    }

    // 프로젝션을 통해 필요한 열만 가져오도록 해야한다.
    public void fetchAuthorsNames2() {
        Streamable<AuthorName> authors = authorRepository.queryByGenre("Anthology");

        authors.forEach(a -> System.out.println(a.getName()));
    }
}
