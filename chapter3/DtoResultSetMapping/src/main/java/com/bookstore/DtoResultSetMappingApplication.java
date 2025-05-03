package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class DtoResultSetMappingApplication {

    private final BookstoreService bookstoreService;

    public DtoResultSetMappingApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DtoResultSetMappingApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            List<String> names = bookstoreService.fetchAuthorsNames();
            System.out.println("Number of items:" + names.size());
            System.out.println(names);
        };
    }
}
