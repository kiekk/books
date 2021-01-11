package com.example.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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
	
	//Query 어노테이션을 사용하여 JPQL 등록
	//JPQL에서 ?1은 첫번째 파라미터를 의미하며, 여기서는 매개변수인 searchKeyword가 바인딩됩니다.
	@Query("SELECT b FROM Board b "
			+ "WHERE b.title like %:searchKeyword% "
			+ "ORDER BY b.seq DESC")
	List<Board> queryAnnotationTest1(@Param("searchKeyword") String searchKeyword);
	
	//특정 변수만 조회하기
	/*
	 * 검색 결과로 엔티티 객체가 조회되는 것이 아니라 여러 변수 값들이 조회되기 때문에 Obejct[] 로 작성합니다.
	 */
	@Query("SELECT b.seq, b.title, b.writer, b.createDate "
			+ "FROM Board b "
			+ "WHERE b.title like %?1% "
			+ "ORDER BY b.seq DESC")
	List<Object[]> queryAnnotationTest2(@Param("searchKeyword") String searchKeyword);
	
	/*
	 * 네이티브 쿼리 테스트
	 */
	@Query(value="select seq, title, writer, createdate "
			+ "from board where title like '%' || ?1 || '%' "
			+ "order by seq desc", nativeQuery=true)
	List<Object[]> queryAnnotationTest3(String searchKeyword);
	
	//@Query 어노테이션을 사용하여 페이징  및 정렬 처리
	@Query("SELECT b FROM Board b ORDER By b.seq DESC")
	List<Board> queryAnnotationTest4(Pageable paging);
}
