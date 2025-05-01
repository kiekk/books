package com.bookstore;

import com.bookstore.dto.AuthorDto;
import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class DtoConstructorApplication {

    private final BookstoreService bookstoreService;

    public DtoConstructorApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(DtoConstructorApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            System.out.println("\n\n Fetch authors by constructor");
            System.out.println("-----------------------------------------------------------------------------");
            List<AuthorDto> authors = bookstoreService.fetchByGenre();
            System.out.println("Number of authors:" + authors.size());

            for (AuthorDto author : authors) {
                System.out.println("Author name: " + author.getName()
                        + " | Age: " + author.getAge());
            }

            System.out.println("\n\n Fetch authors by constructor expression");
            System.out.println("-----------------------------------------------------------------------------");
            List<AuthorDto> authors2 = bookstoreService.fetchAuthors();
            System.out.println("Number of authors2:" + authors2.size());

            for (AuthorDto author : authors2) {
                System.out.println("Author name: " + author.getName()
                        + " | Age: " + author.getAge());
            }
        };
    }
}
