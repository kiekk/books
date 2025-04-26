package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    List<AuthorNameAge> findFirst2ByGenre(String genre);

    interface AuthorNameAge {
        String getName();

        int getAge();
    }
}
