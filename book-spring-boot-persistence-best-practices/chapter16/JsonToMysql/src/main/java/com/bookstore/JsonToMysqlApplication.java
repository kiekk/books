package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JsonToMysqlApplication {

    private final BookstoreService bookstoreService;

    public JsonToMysqlApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(JsonToMysqlApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            System.out.println("\n\nPersist a new author:");
            bookstoreService.newAuthor();

            System.out.println("\n\nFind author by name:");
            bookstoreService.byName();

            System.out.println("\n\nFind author by the isbn of his book:");
            bookstoreService.byNameIsbn();

            System.out.println("\n\nFind author by the isbn of his book via a native query:");
            bookstoreService.byBookIsbnNativeQuery();
        };
    }
}
