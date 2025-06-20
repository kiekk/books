package com.bookstore.repository;

import com.bookstore.dto.BookstoreDto;
import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    @Query(value = """
            SELECT a.name AS name, b.title AS title
                        FROM Author a INNER JOIN Book b ON a.name = b.name
                        WHERE b.price = :price
            """)
    List<BookstoreDto> fetchAuthorNameBookTitleWithPrice(@Param("price") int price);
}
