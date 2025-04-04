package com.bookstore.repository;

import com.bookstore.entity.Publisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PublisherRepository extends JpaRepository<Publisher, Long>, JpaSpecificationExecutor<Publisher> {
    @Override
    @EntityGraph(value = "publisher-books-graph", type = EntityGraph.EntityGraphType.FETCH)
    List<Publisher> findAll();

    @EntityGraph(value = "publisher-books-graph", type = EntityGraph.EntityGraphType.FETCH)
    List<Publisher> findByIdLessThanOrderByCompanyDesc(long id);

    @Override
    @EntityGraph(value = "publisher-books-graph", type = EntityGraph.EntityGraphType.FETCH)
    List<Publisher> findAll(Specification<Publisher> spec);

    @EntityGraph(value = "publisher-books-graph", type = EntityGraph.EntityGraphType.FETCH)
    @Query("SELECT p FROM Publisher p WHERE p.id > 1 AND p.id < 3")
    List<Publisher> fetchAllIdBetween1And3();
}
