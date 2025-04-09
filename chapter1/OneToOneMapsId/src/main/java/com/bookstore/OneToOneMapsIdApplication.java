package com.bookstore;

import com.bookstore.entity.Book;
import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OneToOneMapsIdApplication {

    private final BookstoreService bookstoreService;

    public OneToOneMapsIdApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(OneToOneMapsIdApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            bookstoreService.newBookOfAuthor();

            Book book = bookstoreService.fetchBookByAuthorId();
            System.out.println(book);

            book = bookstoreService.fetchBookByAuthor();
            System.out.println(book);
        };
    }
}
