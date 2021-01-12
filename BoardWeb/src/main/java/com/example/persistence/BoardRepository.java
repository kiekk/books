package com.example.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;

import com.example.domain.Board;
import com.querydsl.core.BooleanBuilder;

public interface BoardRepository extends CrudRepository<Board, Long>, QuerydslPredicateExecutor<Board> {
	
	//글 목록 검색
	@Query("SELECT b FROM Board b")
	Page<Board> getBoardList(Pageable pageable);

}
