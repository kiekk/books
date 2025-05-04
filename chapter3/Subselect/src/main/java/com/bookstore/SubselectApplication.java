package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SubselectApplication {

    private final BookstoreService bookstoreService;

    public SubselectApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(SubselectApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> bookstoreService.fetchAuthorWithBooksById(4, "History");
    }
}
