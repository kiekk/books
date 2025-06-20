package com.bookstore.repository;

import com.bookstore.entity.AuthorShallow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorShallowRepository extends JpaRepository<AuthorShallow, Long> {
}
