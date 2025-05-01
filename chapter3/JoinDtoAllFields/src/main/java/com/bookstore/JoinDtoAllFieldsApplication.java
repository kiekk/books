package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JoinDtoAllFieldsApplication {

    private final BookstoreService bookstoreService;

    public JoinDtoAllFieldsApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(JoinDtoAllFieldsApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            System.out.println("\n\n Fetch authors read-only entities:");
            System.out.println("-----------------------------------------------------------------------------");
            bookstoreService.fetchAuthorAsReadOnlyEntities();

            System.out.println("\n\n Fetch authors as projection");
            System.out.println("-----------------------------------------------------------------------------");
            bookstoreService.fetchAuthorAsProjection();

            System.out.println("\n\n Fetch authors as array of objects");
            System.out.println("-----------------------------------------------------------------------------");
            bookstoreService.fetchAuthorAsArrayOfObject();
        };
    }

}
