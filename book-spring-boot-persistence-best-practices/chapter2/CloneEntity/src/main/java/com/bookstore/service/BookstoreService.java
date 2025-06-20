package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import com.bookstore.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;

    public BookstoreService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional
    public void init() {
        Author author = Author.createAuthor("Mark Janel", 23, "Anthology");
        Book book1 = Book.createBook("My Anthology", "001");
        Book book2 = Book.createBook("999 Anthology", "002");

        author.addBook(book1);
        author.addBook(book2);

        authorRepository.save(author);
    }

    @Transactional
    public void cloneAuthor() {
        Author author = authorRepository.fetchByName("Mark Janel");

        Author authorClone = new Author(author, false);
        authorClone.changeAge(54);
        authorClone.changeName("Farell Tliop");

        // cloneChildren: false일 경우 addbook() 대신 books.addAll() 메서드를 사용하여
        // addBook 내부의 book.getAuthors().add(this)에 의해 실행되는 select 쿼리를 방지합니다.
        System.out.println("=== books ===");
        authorClone.getBooks().forEach(
                b -> System.out.println(b.getAuthors()));

        authorRepository.save(authorClone);
    }
}
