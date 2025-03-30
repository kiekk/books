package com.bookstore.service;

import com.bookstore.entity.Author;
import com.bookstore.entity.Book;
import com.bookstore.repository.AuthorRepository;
import com.bookstore.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookstoreService {
    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public BookstoreService(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public void insertNewBook() {
        Long authorId = 4L;
        Author author = authorRepository.getReferenceById(authorId);

        Book book = Book.createBook("003-JN", "History Of Present", author);

        bookRepository.save(book);

        // dirty checking
        // update 쿼리가 실행된다.
        book.changeIsbn("not available");
    }

    @Transactional(readOnly = true)
    public void fetchBooksOfAuthorById() {
        List<Book> books = bookRepository.fetchBooksOfAuthorById(4L);

        System.out.println(books);

        // dirty checking
        // update 쿼리가 실행된다.
        books.getFirst().changeIsbn("not available");
    }

    @Transactional(readOnly = true)
    public void fetchPageBooksOfAuthorById() {
        long authorId = 4L;
        Page<Book> books = bookRepository.fetchPageBooksOfAuthorById(authorId,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "title")));

        books.get().forEach(System.out::println);
    }

    @Transactional
    public void fetchBooksOfAuthorByIdAndAddNewBook() {
        long authorId = 4L;
        List<Book> books = bookRepository.fetchBooksOfAuthorById(authorId);

        Book book = Book.createBook("004-JN", "History Facts", books.get(0).getAuthor());

        books.add(bookRepository.save(book));

        System.out.println(books);
    }

    @Transactional
    public void fetchBooksOfAuthorByIdAndDeleteFirstBook() {
        long authorId = 4L;
        List<Book> books = bookRepository.fetchBooksOfAuthorById(authorId);

        bookRepository.delete(books.remove(0));

        System.out.println(books);
    }
}
