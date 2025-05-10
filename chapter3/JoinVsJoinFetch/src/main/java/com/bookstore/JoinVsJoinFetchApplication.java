package com.bookstore;

import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JoinVsJoinFetchApplication {

	private final BookstoreService bookstoreService;

    public JoinVsJoinFetchApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
		SpringApplication.run(JoinVsJoinFetchApplication.class, args);
	}

	@Bean
	public ApplicationRunner init() {
		return args -> {
			System.out.println("\n\nfetchAuthorsBooksByPriceInnerJoin (notice that we fetch all books of each author): ");
			bookstoreService.fetchAuthorsBooksByPriceInnerJoin();

			System.out.println("\n\nfetchAuthorsBooksByPriceJoinFetch: ");
			bookstoreService.fetchAuthorsBooksByPriceJoinFetch();
		};
	}
}
