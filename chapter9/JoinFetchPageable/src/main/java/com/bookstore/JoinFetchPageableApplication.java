package com.bookstore;

import com.bookstore.entity.Author;
import com.bookstore.service.BookstoreService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;

@SpringBootApplication
public class JoinFetchPageableApplication {

	private final BookstoreService bookstoreService;

    public JoinFetchPageableApplication(BookstoreService bookstoreService) {
        this.bookstoreService = bookstoreService;
    }

    public static void main(String[] args) {
		SpringApplication.run(JoinFetchPageableApplication.class, args);
	}

	@Bean
	public ApplicationRunner init() {
		return args -> {
			Page<Author> authorscq = bookstoreService.fetchWithBooksByGenreCQ();
			authorscq.forEach(a -> System.out.println(a.getName() + ": " + a.getBooks()));
			System.out.println("Number of elements: " + authorscq.getNumberOfElements());
			System.out.println("Total elements: " + authorscq.getTotalElements());

			Page<Author> authorseg = bookstoreService.fetchWithBooksByGenreEG();
			authorseg.forEach(a -> System.out.println(a.getName() + ": " + a.getBooks()));
			System.out.println("Number of elements: " + authorseg.getNumberOfElements());
			System.out.println("Total elements: " + authorseg.getTotalElements());
		};
	}
}
