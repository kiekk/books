package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StreamAndMySqlApplication {

    private final BookstoreService bookstoreService;

    public StreamAndMySqlApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(StreamAndMySqlApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
//            bookstoreService.populateDatabase();

//            System.out.println("\nStreaming: ");
//            bookstoreService.streamDatabase();
//
//            System.out.println("\nStreaming2: ");
//            bookstoreService.streamDatabase2();
//
//            System.out.println("\n\nFetch Author as Streamable:");
//            bookstoreService.fetchAuthorsAsStreamable();
//
//            System.out.println("\n\nCAUTION! Concatenating two Streamable:");
//            bookstoreService.fetchAuthorsByGenreConcatAge();

            System.out.println("\n\nDON'T DO THIS! Fetch all columns just to drop a part of them:");
            bookstoreService.fetchAuthorsNames1();

            System.out.println("\n\nDO THIS! Fetch only the needed columns:");
            bookstoreService.fetchAuthorsNames2();
        };
    }
}
