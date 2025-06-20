package com.bookstore;

import com.bookstore.dto.AuthorDto;
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

            System.out.println("============================================");

            List<AuthorDto> authors = bookstoreService.fetchAuthorsNamesAndAges();
            System.out.println("Number of authors:" + authors.size());

            for (AuthorDto author : authors) {
                System.out.println("Author name: " + author.getName()
                        + " | Age: " + author.getAge());
            }
        };
    }
}
