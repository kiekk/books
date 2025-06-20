package com.bookstore;

import com.bookstore.service.BookstoreService;
import jakarta.persistence.Tuple;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class DtoTupleApplication {

    private final BookstoreService bookstoreService;

    public DtoTupleApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DtoTupleApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            List<Tuple> authors = bookstoreService.fetchAuthors();
            System.out.println("Number of authors:" + authors.size());

            for (Tuple author : authors) {
                System.out.println("Author name: " + author.get("name")
                        + " | Age: " + author.get("age"));
            }
            System.out.println("\n------------------");
            Tuple author = authors.getFirst();
            System.out.println(author.get("name") instanceof String);
            System.out.println(author.get("age") instanceof Integer);
        };
    }
}
