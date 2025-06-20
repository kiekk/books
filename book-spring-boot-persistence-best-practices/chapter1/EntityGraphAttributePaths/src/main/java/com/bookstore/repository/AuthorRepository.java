package com.bookstore.repository;

import com.bookstore.entity.Author;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorRepository extends JpaRepository<Author, Long>, JpaSpecificationExecutor<Author> {

    // fetch graph: attributeNodes에 지정되는 속성들은 FetchType.EAGER로 처리
    // 나머지 속성들은 FetchType.LAZY로 처리 (기본 설정 또는 명시적 설정과 관계 X)
    @EntityGraph(attributePaths = {"books"}, type = EntityGraph.EntityGraphType.FETCH)
    @Override
    List<Author> findAll();

    // 쿼리 빌더에도 적용 가능
    @EntityGraph(attributePaths = {"books"},
            type = EntityGraph.EntityGraphType.FETCH)
    List<Author> findByAgeLessThanOrderByNameDesc(int age);

    @Override
    @EntityGraph(attributePaths = {"books"}, type = EntityGraph.EntityGraphType.FETCH)
    List<Author> findAll(Specification<Author> spec);

    @EntityGraph(attributePaths = {"books"}, type = EntityGraph.EntityGraphType.FETCH)
    @Query(value = "SELECT a FROM Author a WHERE a.age > 20 AND a.age < 40")
    List<Author> fetchAllAgeBetween20And40();

}
