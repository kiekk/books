package com.bookstore.service;

import com.bookstore.entity.AuthorList;
import com.bookstore.entity.AuthorSet;
import com.bookstore.entity.BookList;
import com.bookstore.entity.BookSet;
import com.bookstore.repository.AuthorListRepository;
import com.bookstore.repository.AuthorSetRepository;
import com.bookstore.repository.BookSetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {

    private final AuthorListRepository authorListRepository;
    private final AuthorSetRepository authorSetRepository;
    private final BookSetRepository bookSetRepository;

    public BookstoreService(AuthorListRepository authorListRepository, AuthorSetRepository authorSetRepository, BookSetRepository bookSetRepository) {
        this.authorListRepository = authorListRepository;
        this.authorSetRepository = authorSetRepository;
        this.bookSetRepository = bookSetRepository;
    }

    @Transactional
    public void persistAuthorWithBooksAndRemoveOneBookList() {
        AuthorList alicia = AuthorList.createAuthor("Alicia Tom", "Anthology", 38);
        AuthorList mark = AuthorList.createAuthor("Mark Janel", "Anthology", 23);

        BookList bookOfSwords = BookList.createBook("The book of swords", "001-AT-MJ");
        BookList oneDay = BookList.createBook("One Day", "002-AT-MJ");
        BookList headDown = BookList.createBook("Head Down", "001-AT");

        alicia.addBook(bookOfSwords);
        mark.addBook(bookOfSwords);
        alicia.addBook(oneDay);
        mark.addBook(oneDay);
        alicia.addBook(headDown);

        authorListRepository.save(alicia);
        authorListRepository.saveAndFlush(mark);

        System.out.println("================================================");
        System.out.println("Removing a book (List case) ...");
        System.out.println("================================================");


        alicia.removeBook(oneDay);
    }

    @Transactional
    public void persistAuthorWithBooksAndRemoveOneBookSet() {
        AuthorSet alicia = AuthorSet.createAuthor("Alicia Tom", "Anthology", 38);
        AuthorSet mark = AuthorSet.createAuthor("Mark Janel", "Anthology", 23);

        BookSet bookOfSwords = BookSet.createBook("The book of swords", "001-AT-MJ");
        BookSet oneDay = BookSet.createBook("One Day", "002-AT");
        BookSet headDown = BookSet.createBook("Head Down", "001-AT");

        alicia.addBook(bookOfSwords);
        mark.addBook(bookOfSwords);
        alicia.addBook(oneDay);
        mark.addBook(oneDay);
        alicia.addBook(headDown);

        authorSetRepository.save(alicia);
        authorSetRepository.saveAndFlush(mark);

        System.out.println("================================================");
        System.out.println("Removing a book (Set case) ...");
        System.out.println("================================================");

        alicia.removeBook(oneDay);
    }

    @Transactional
    public void getAuthorSetByBookId() {
        BookSet bookSet = bookSetRepository.findById(1L).orElseThrow();
        bookSet.getAuthors().forEach(System.out::println);
    }
}
