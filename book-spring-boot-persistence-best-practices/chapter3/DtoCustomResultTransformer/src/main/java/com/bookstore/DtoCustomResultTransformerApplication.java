package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DtoCustomResultTransformerApplication {

    private final BookstoreService bookstoreService;

    public DtoCustomResultTransformerApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DtoCustomResultTransformerApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> bookstoreService.fetchAuthorWithBook();
    }
}
