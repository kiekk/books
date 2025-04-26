package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProjectionApplication {

    private final BookstoreService bookstoreService;

    public ProjectionApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(ProjectionApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            System.out.println("\n\n Fetch all:");
            System.out.println("---------------------------------");
            bookstoreService.fetchAll();

            System.out.println("\n\n Fetch age, name and genre:");
            System.out.println("---------------------------------");
            bookstoreService.fetchAgeNameGenre();

            System.out.println("\n\n Fetch name and email:");
            System.out.println("---------------------------------");
            bookstoreService.fetchNameEmail();
        };
    }
}
