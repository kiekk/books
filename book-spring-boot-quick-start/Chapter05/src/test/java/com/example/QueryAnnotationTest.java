package com.example;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;

import com.example.domain.Board;
import com.example.persistence.BoardRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class QueryAnnotationTest {
	@Autowired
	private BoardRepository boardRepo;
	
	/*
	public void testQueryAnnotationTest1() {
		List<Board> boardList = boardRepo.queryAnnotationTest1("테스트 제목 10");
		
		System.out.println("=== 검색 결과 ===");
		boardList.forEach(System.out::println);
	}
	
	public void testQueryAnnotationTest2() {
		List<Object[]> boardList = boardRepo.queryAnnotationTest2("테스트 제목 10");
		
		System.out.println("=== 검색 결과 ===");
		boardList.forEach(row -> {
			System.out.println(Arrays.toString(row));
		});
	}
	
	public void testQueryAnnotationTest3() {
		List<Object[]> boardList = boardRepo.queryAnnotationTest3("테스트 제목 10");
		
		System.out.println("=== 검색 결과 ===");
		boardList.forEach(row -> {
			System.out.println(Arrays.toString(row));
		});
	}
	
	//@Query 어노테이션을 사용하여 페이징  및 정렬 처리
	@Test
	public void testQueryAnnotationTest4() {
		Pageable paging = PageRequest.of(0, 3, Sort.Direction.DESC, "seq");
		List<Board> boardList = boardRepo.queryAnnotationTest4(paging);
		
		System.out.println("=== 검색 결과 ===");
		boardList.forEach(System.out::println);
	}
	*/
}
