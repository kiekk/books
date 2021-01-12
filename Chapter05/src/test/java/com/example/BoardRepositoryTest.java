package com.example;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.example.domain.Board;
import com.example.persistence.BoardRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BoardRepositoryTest {
	@Autowired
	private BoardRepository boardRepo;
	
	//글 등록
	public void testInsertBoard() {
		Board board = new Board();
		board.setTitle("첫 번째 게시글");
		//board.setWriter("테스터");
		board.setContent("첫 번째 게시글 내용");
		board.setCreateDate(new Date());
		board.setCnt(0L);
		
		boardRepo.save(board);
		/*
		 * 원래 JPA의 persist() 메소드를 사용했지만, CrudRepository 인터페이스를 사용할 때에는
		 * save() 메소드를 사용합니다.
		 */
	}
	
	//상세 조회
	public void testGetBoard() {
		Board board = boardRepo.findById(1L).get();
		System.out.println(board.toString());
	}
	
	//글 수정
	public void testUpdateBoard() {
		Long seq = 1L;
		System.out.println("=== " + seq + "번 게시글 조회 ===");
		Board board = boardRepo.findById(seq).get();
		
		System.out.println("=== " + seq + "번 게시글 제목 수정 ===");
		board.setTitle("수정 제목입니다.");
		boardRepo.save(board);
	}
	
	//글 삭제
	@Test
	public void testDeletBoard() {
		boardRepo.deleteById(1L);
	}
}
