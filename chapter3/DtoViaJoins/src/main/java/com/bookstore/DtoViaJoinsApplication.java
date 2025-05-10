package com.bookstore;

import com.bookstore.service.BookstoreServiceCrossJoin;
import com.bookstore.service.BookstoreServiceInnerJoin;
import com.bookstore.service.BookstoreServiceRightJoin;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DtoViaJoinsApplication {

    private final BookstoreServiceInnerJoin bookstoreServiceInnerJoin;
    private final BookstoreServiceRightJoin bookstoreServiceRightJoin;
    private final BookstoreServiceCrossJoin bookstoreServiceCrossJoin;

    public DtoViaJoinsApplication(BookstoreServiceInnerJoin bookstoreServiceInnerJoin, BookstoreServiceRightJoin bookstoreServiceRightJoin, BookstoreServiceCrossJoin bookstoreServiceCrossJoin) {
        this.bookstoreServiceInnerJoin = bookstoreServiceInnerJoin;
        this.bookstoreServiceRightJoin = bookstoreServiceRightJoin;
        this.bookstoreServiceCrossJoin = bookstoreServiceCrossJoin;
    }

    public static void main(String[] args) {
        SpringApplication.run(DtoViaJoinsApplication.class, args);
    }

    @Bean
    public ApplicationRunner init() {
        return args -> {
//            System.out.println("== Inner Join ==");
//            innerJoin();

//            System.out.println("== Right Join ==");
//            rightJoin();

            System.out.println("== Cross Join ==");
            crossJoin();
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

    private void rightJoin() {
        System.out.println("\nfetchBooksAndAuthorsJpql: ");
        bookstoreServiceRightJoin.fetchBooksAndAuthorsJpql()
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfetchBooksAndAuthorsSql: ");
        bookstoreServiceRightJoin.fetchBooksAndAuthorsSql()
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfetchAuthorsAndBooksJpql: ");
        bookstoreServiceRightJoin.fetchAuthorsAndBooksJpql()
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));

        System.out.println("\nfetchAuthorsAndBooksSql: ");
        bookstoreServiceRightJoin.fetchAuthorsAndBooksSql()
                .forEach((e) -> System.out.println(e.getName() + " | " + e.getTitle()));
    }

    private void crossJoin() {
        System.out.println("\nfetchBooksAndFormatsJpql: ");
        bookstoreServiceCrossJoin.fetchBooksAndFormatsJpql()
                .forEach((e) -> System.out.println(e.getTitle() + " | " + e.getFormatType()));

        System.out.println("\nfetchBooksAndFormatsSql: ");
        bookstoreServiceCrossJoin.fetchBooksAndFormatsSql()
                .forEach((e) -> System.out.println(e.getTitle() + " | " + e.getFormatType()));

        System.out.println("\nfetchFormatsAndBooksJpql: ");
        bookstoreServiceCrossJoin.fetchFormatsAndBooksJpql()
                .forEach((e) -> System.out.println(e.getTitle() + " | " + e.getFormatType()));

        System.out.println("\nfetchFormatsAndBooksSql: ");
        bookstoreServiceCrossJoin.fetchFormatsAndBooksSql()
                .forEach((e) -> System.out.println(e.getTitle() + " | " + e.getFormatType()));
    }
}
