package com.bookstore.repository;

import com.bookstore.summary.AuthorSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorSummaryRepository extends JpaRepository<AuthorSummary, Long> {
}
