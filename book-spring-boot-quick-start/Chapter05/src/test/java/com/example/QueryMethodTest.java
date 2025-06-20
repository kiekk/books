package com.example;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;

import com.example.domain.Board;
import com.example.persistence.BoardRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QueryMethodTest {
	@Autowired
	private BoardRepository boardRepo;
	
	@Before
	public void dataPrepare() {
		for(int i=1;i<=200;i++) {
			Board board = new Board();
			board.setTitle("테스트 제목 " + i);
			//board.setWriter("테스터");
			board.setContent("테스트 내용 " + i);
			board.setCreateDate(new Date());
			board.setCnt(0L);
			boardRepo.save(board);
		}
	}
	
	//제목으로 조회
	public void testFindByTitle() {
		List<Board> boardList = boardRepo.findByTitle("테스트 제목 10");
		
		System.out.println("=== 검색 결과 ===");
		boardList.forEach(System.out::println);
	}
	
	//글 내용에 특정 단어가 포함된 목록 검색
	public void testByContentContaining() {
		List<Board> boardList = boardRepo.findByContentContaining("17");
		
		System.out.println("=== 검색 결과 ===");
		boardList.forEach(System.out::println);
	}
	
	//글 내용에 특정 단어가 포함된 목록 Or 글 제목에 특정 단어가 포함된 목록 검색
	public void testFineByTitleContainingOrContentContaining() {
		List<Board> boardList = boardRepo.findByTitleContainingOrContentContaining("17", "17");
		
		System.out.println("=== 검색 결과 ===");
		boardList.forEach(System.out::println);
	}
	
	//정렬하기
	public void testFindByTitleContainingOrderBySeqDesc() {
		List<Board> boardList = boardRepo.findByTitleContainingOrderBySeqDesc("17");
		
		System.out.println("=== 검색 결과 ===");
		boardList.forEach(System.out::println);
	}
	
	//페이징 처리
//	public void testFindByTitleContaining() {
//		Pageable paging = PageRequest.of(0, 5);
//		List<Board> boardList = boardRepo.findByTitleContaining("제목", paging);
//		
//		System.out.println("=== 검색 결과 ===");
//		boardList.forEach(System.out::println);
//	}
	
	//페이징 처리에서 데이터 정렬, List<T> 사용
//	public void testFindByTitleContainingSort() {
//		//PageRequest.of()메소드에 매개변수가 추가됩니다.
//		//첫 번째는 정렬 방향에 대한 정보, 두 번째는 정렬 대상이 되는 변수 이름입니다.
//		Pageable paging = PageRequest.of(0, 5, Sort.Direction.DESC, "seq");
//		List<Board> boardList = boardRepo.findByTitleContaining("제목", paging);
//		
//		System.out.println("=== 검색 결과 ===");
//		boardList.forEach(System.out::println);
//	}
	
	//페이징 처리에서 데이터 정렬, Page<T> 사용
	@Test
	public void testFineByTitleContaining() {
		Pageable paging = PageRequest.of(0, 5, Sort.Direction.DESC, "seq");
		
		Page<Board> pageInfo = boardRepo.findByTitleContaining("제목", paging);
		
		/*
		 * Page<T>를 사용하면 웹 페이지에서 페이징 처리에 필요한 다양한 정보를 아래와 같이 한번에 얻을 수 있습니다.
		 * 때문에 따로 페이징 정보를 위한 SQL문을 매번 작성할 필요가 없습니다.
		 */
		System.out.println("PAGE SIZE : " + pageInfo.getSize());
		System.out.println("TOTAL PAGES : " + pageInfo.getTotalPages());
		System.out.println("TOTAL COUNT : " + pageInfo.getTotalElements());
		System.out.println("NEXT : " + pageInfo.nextPageable());
		
		List<Board> boardList = pageInfo.getContent();
		
		System.out.println("=== 검색 결과 ===");
		boardList.forEach(System.out::println);
	}
}
