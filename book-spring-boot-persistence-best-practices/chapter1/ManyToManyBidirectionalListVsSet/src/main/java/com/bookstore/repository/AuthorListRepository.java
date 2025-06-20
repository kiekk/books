package com.bookstore.repository;

import com.bookstore.entity.AuthorList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorListRepository extends JpaRepository<AuthorList, Long> {
}
