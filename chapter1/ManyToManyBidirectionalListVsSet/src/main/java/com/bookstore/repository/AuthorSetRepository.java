package com.bookstore.repository;

import com.bookstore.entity.AuthorSet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorSetRepository extends JpaRepository<AuthorSet, Long> {
}
