package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CascadeChildRemovalApplication {

    private final BookstoreService bookstoreService;

    public CascadeChildRemovalApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
        SpringApplication.run(CascadeChildRemovalApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
//            System.out.println("\n\n deleteViaCascadeRemove()");
//            bookstoreService.deleteViaCascadeRemove();

//            System.out.println("\n\n deleteViaOrphanRemoval()");
//            bookstoreService.deleteViaOrphanRemoval();

             System.out.println("\n\n deleteViaIdentifiers()");
             bookstoreService.deleteViaIdentifiers();
        };
    }

}
