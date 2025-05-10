package com.bookstore;

import com.bookstore.dto.BookstoreDto;
import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class DtoUnrelatedEntitiesApplication {

    private final BookstoreService bookstoreService;

    public DtoUnrelatedEntitiesApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DtoUnrelatedEntitiesApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            List<BookstoreDto> result = bookstoreService.fetchAuthorNameBookTitleWithPrice(42);

            result.forEach(r -> System.out.println("Author: " + r.getName() + "  Title: " + r.getTitle()));
        };
    }
}
