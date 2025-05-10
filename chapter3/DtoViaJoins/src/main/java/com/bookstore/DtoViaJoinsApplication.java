package com.bookstore;

import com.bookstore.service.BookstoreServiceInnerJoin;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DtoViaJoinsApplication {

    private final BookstoreServiceInnerJoin bookstoreServiceInnerJoin;

    public DtoViaJoinsApplication(BookstoreServiceInnerJoin bookstoreServiceInnerJoin) {
        this.bookstoreServiceInnerJoin = bookstoreServiceInnerJoin;
    }

    public static void main(String[] args) {
        SpringApplication.run(DtoViaJoinsApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
            System.out.println("== Inner Join ==");
            innerJoin();
        };
    }

    private void innerJoin() {
        System.out.println("\nfetchBooksAndAuthorsJpql: ");
        bookstoreServiceInnerJoin.fetchBooksAndAuthorsJpql()
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfetchBooksAndAuthorsSql: ");
        bookstoreServiceInnerJoin.fetchBooksAndAuthorsSql()
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfetchAuthorsAndBooksJpql: ");
        bookstoreServiceInnerJoin.fetchAuthorsAndBooksJpql()
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfetchAuthorsAndBooksSql: ");
        bookstoreServiceInnerJoin.fetchAuthorsAndBooksSql()
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfindAuthorsAndBooksByGenreAndPriceJpql: ");
        bookstoreServiceInnerJoin.findAuthorsAndBooksByGenreAndPriceJpql("History", 40)
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfindAuthorsAndBooksByGenreAndPriceSql: ");
        bookstoreServiceInnerJoin.findAuthorsAndBooksByGenreAndPriceSql("History", 40)
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfindBooksAndAuthorsByGenreAndPriceJpql: ");
        bookstoreServiceInnerJoin.findBooksAndAuthorsByGenreAndPriceJpql("History", 40)
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfindBooksAndAuthorsByGenreAndPriceSql: ");
        bookstoreServiceInnerJoin.findBooksAndAuthorsByGenreAndPriceSql("History", 40)
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));
    }
}
