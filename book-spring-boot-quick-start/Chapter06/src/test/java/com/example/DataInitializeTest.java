package com.example;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.example.domain.Board;
import com.example.domain.Member;
import com.example.persistence.BoardRepository;
import com.example.persistence.MemberRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DataInitializeTest {
	@Autowired
	private MemberRepository memberRepo;
	
	@Autowired
	private BoardRepository boardRepo;
	
	@Test
	public void testDataInsert() {
		Member member1 = new Member();
		member1.setId("member1");
		member1.setName("apple");
		member1.setPassword("member111");
		member1.setRole("ROLE_USER");
		memberRepo.save(member1);
		
		Member member2 = new Member();
		member2.setId("member2");
		member2.setName("banana");
		member2.setPassword("member222");
		member2.setRole("ROLE_ADMIN");
		memberRepo.save(member2);
		
		for(int i=1;i<=3;i++) {
			Board board = new Board();
			board.setWriter("apple");
			board.setTitle("apple이 등록한 게시글 " + i);
			board.setContent("apple이 등록한 게시글 내용 " + i);
			boardRepo.save(board);
		}
		
		for(int i=1;i<=3;i++) {
			Board board = new Board();
			board.setWriter("banana");
			board.setTitle("banana이 등록한 게시글 " + i);
			board.setContent("banana이 등록한 게시글 내용 " + i);
			boardRepo.save(board);
		}
		
	}
}
