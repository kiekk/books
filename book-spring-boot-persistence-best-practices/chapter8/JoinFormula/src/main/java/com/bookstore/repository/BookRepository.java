package com.bookstore.repository;

import com.bookstore.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BookRepository extends JpaRepository<Book, Long> {
    @Query(value="SELECT * FROM book WHERE price < ?1 AND author_id = ?2 "
            + "ORDER BY price DESC LIMIT 1",
            nativeQuery = true)
    Book fetchNextSmallerPrice(int price, long authorId);
}
