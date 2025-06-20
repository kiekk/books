package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PopulatingChildViaProxyApplication {

    private final BookstoreService bookstoreService;

    public PopulatingChildViaProxyApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(PopulatingChildViaProxyApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            System.out.println("\nCall addBookToAuthor();");
            bookstoreService.addBookToAuthor();

            System.out.println("\nCall addBookToAuthorWithFindById();");
            bookstoreService.addBookToAuthorWithFindById();
        };
    }

}
