package com.bookstore;

import com.bookstore.impl.BatchRepositoryImpl;
import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(repositoryBaseClass = BatchRepositoryImpl.class)
public class BatchInsertOrderApplication {

    private final BookstoreService bookstoreService;

    public BatchInsertOrderApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(BatchInsertOrderApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            bookstoreService.batchAuthorsAndBooks();
        };
    }
}
