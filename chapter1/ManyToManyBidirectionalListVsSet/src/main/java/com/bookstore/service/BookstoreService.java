package com.bookstore.service;

import com.bookstore.entity.AuthorList;
import com.bookstore.entity.BookList;
import com.bookstore.repository.AuthorListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookstoreService {

    private final AuthorListRepository authorListRepository;

    public BookstoreService(AuthorListRepository authorListRepository) {
        this.authorListRepository = authorListRepository;
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
}
