package com.example.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import com.example.domain.Board;

public interface BoardRepository extends CrudRepository<Board, Long> {
	//글 제목으로 검색
	List<Board> findByTitle(String searchKeyword);
	
	//글 내용에 특정 단어가 포함된 목록 검색
	List<Board> findByContentContaining(String searchKeyword);
	
	/*
	 * title과 content 두 개의 변수에 대해서 제약조건을 추가했기 때문에 검색 키워드가 동일하더라도
	 * 매개변수를 반드시 각각 작성해야 합니다.
	 */
	//글 내용에 특정 단어가 포함된 목록 Or 글 제목에 특정 단어가 포함된 목록 검색
	List<Board> findByTitleContainingOrContentContaining(String title, String content);
	
	//정렬하기
	List<Board> findByTitleContainingOrderBySeqDesc(String searchKeyword);
	
	//페이징과 정렬
	//한 화면에 5개씩 보여주기로 설정
	//List<Board> findByTitleContaining(String searchKeyword, Pageable paging);
	
	/*
	 * 만약 스프링 MVC에서 검색 결과를 사용할 목적이라면, List<T>보다는 Page<T>가 더 용이합니다.
	 * Page<T> 객체는 페이징 처리할때 사용할 수 있는 다양한 정보들을 추가로 제공합니다.
	 */
	Page<Board> findByTitleContaining(String searchKeyword, Pageable paging);
}
