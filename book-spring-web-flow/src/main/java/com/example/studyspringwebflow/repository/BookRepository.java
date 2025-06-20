package com.example.studyspringwebflow.repository;

import com.example.studyspringwebflow.entity.Book;
import com.example.studyspringwebflow.entity.BookSearchCriteria;
import com.example.studyspringwebflow.entity.Category;

import java.util.List;

public interface BookRepository {

	Book findById(long id);

	List<Book> findByCategory(Category category);

	List<Book> findRandom(int count);

	List<Book> findBooks(BookSearchCriteria bookSearchCriteria);

	void storeBook(Book book);

}
