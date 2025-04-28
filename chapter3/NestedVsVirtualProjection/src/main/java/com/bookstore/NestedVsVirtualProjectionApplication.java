package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NestedVsVirtualProjectionApplication {

    private final BookstoreService bookstoreService;

    public NestedVsVirtualProjectionApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(NestedVsVirtualProjectionApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            System.out.println("\n\nFetch books with authors via query builder mechanism");
            System.out.println("-----------------------------------------------------------------");
            bookstoreService.fetchBooksWithAuthorsQueryBuilderMechanism();

            System.out.println("\n\nFetch books with authors via JPQL query");
            System.out.println("-----------------------------------------------------------------");
            bookstoreService.fetchBooksWithAuthorsViaQuery();
        };
    }
}
