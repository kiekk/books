package com.example.studyspringwebflow.service;

import com.example.studyspringwebflow.entity.*;

import java.util.List;

public interface BookstoreService {

    List<Book> findBooksByCategory(Category category);

    Book findBook(long id);

    Order findOrder(long id);

    List<Book> findRandomBooks();

    List<Order> findOrdersForAccount(Account account);

    Order store(Order order);

    List<Book> findBooks(BookSearchCriteria bookSearchCriteria);

    Order createOrder(Cart cart, Account account);

    List<Category> findAllCategories();

    void addBook(Book book);
}
