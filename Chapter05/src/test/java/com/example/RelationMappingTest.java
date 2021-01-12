package com.example;

import java.util.Date;
import java.util.List;

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
public class RelationMappingTest {
	@Autowired
	private BoardRepository boardRepo;
	
	@Autowired
	private MemberRepository memberRepo;
	
	
	//Member와 Board객체 관계 설정하여 게시글 등록
	public void testManyToOneInsert() {
		Member member1 = new Member();
		member1.setId("member1");
		member1.setPassword("member111");
		member1.setName("apple");
		member1.setRole("User");
		//memberRepo.save(member1);
		
		Member member2 = new Member();
		member2.setId("member2");
		member2.setPassword("member222");
		member2.setName("banana");
		member2.setRole("Admin");
		//memberRepo.save(member2);
		
		for(int i=1;i<=3;i++) {
			Board board = new Board();
			board.setMember(member1);
			board.setTitle("apple이 등록한 게시글 " + i);
			board.setContent("apple이 등록한 게시글 내용 " + i);
			board.setCreateDate(new Date());
			board.setCnt(0L);
			//boardRepo.save(board);
		}
		memberRepo.save(member1);
		
		for(int i=1;i<=3;i++) {
			Board board = new Board();
			board.setMember(member2);
			board.setTitle("banana이 등록한 게시글 " + i);
			board.setContent("banana이 등록한 게시글 내용 " + i);
			board.setCreateDate(new Date());
			board.setCnt(0L);
			//boardRepo.save(board);
		}
		memberRepo.save(member2);
	}
	
	//Member와 Board객체 관계 설정하여 게시글 조회
	public void testManyToOneSelect() {
		Board board = boardRepo.findById(5L).get();
		System.out.println("[ " + board.getSeq() + "번 게시글 정보 ]");
		System.out.println("제목 : " + board.getTitle());
		System.out.println("내용 : " + board.getContent());
		System.out.println("작성자 : " + board.getMember().getName());
		System.out.println("작성자 권한 : " + board.getMember().getRole());
	}
	
	//Member와 Board 양방향 관계 테스트
	public void testTwoWayMapping() {
		Member member = memberRepo.findById("member1").get();
		
		System.out.println("========================");
		System.out.println(member.getName() + "가(이) 저장한 게시글 목록");
		System.out.println("========================");
		
		List<Board> list = member.getBoardList();
		list.forEach(System.out::println);
	}
	
	//영속성 전이를 이용한 삭제
	@Test
	public void testCascadeDelete() {
		memberRepo.deleteById("member2");
	}
}
