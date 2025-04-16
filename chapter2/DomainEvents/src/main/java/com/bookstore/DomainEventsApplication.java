package com.bookstore;

import com.bookstore.entity.BookReview;
import com.bookstore.entity.ReviewStatus;
import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.logging.Logger;

@SpringBootApplication
public class DomainEventsApplication {

    private static final Logger logger = Logger.getLogger(DomainEventsApplication.class.getName());

    private final BookstoreService bookstoreService;

    public DomainEventsApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DomainEventsApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            bookstoreService.insertBook();

            BookReview bookReview = BookReview.createBookReview("Very good book!", "marinv@gmail.com", ReviewStatus.CHECK);

            String response = bookstoreService.postReview(bookReview);
            logger.info(() -> "Response: " + response);
        };
    }
}
