package com.example;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

import com.example.domain.Board;
import com.example.domain.Member;
import com.example.domain.Role;
import com.example.persistence.BoardRepository;
import com.example.persistence.MemberRepository;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BoardRepositoryTest {
	@Autowired
	private BoardRepository boardRepo;
	
	@Autowired
	private MemberRepository memberRepo;
	
	@Autowired
	private PasswordEncoder encoder;
	
	//더미데이터 추가
	@Test
	public void testInsert() {
		Member member1 = new Member();
		member1.setId("member");
		member1.setPassword(encoder.encode("member123"));
		member1.setName("apple");
		member1.setRole(Role.ROLE_MEMBER);
		member1.setEnabled(true);
		memberRepo.save(member1);
		
		Member member2 = new Member();
		member2.setId("admin");
		member2.setPassword(encoder.encode("admin123"));
		member2.setName("banana");
		member2.setRole(Role.ROLE_ADMIN);
		member2.setEnabled(true);
		memberRepo.save(member2);
		
		for(int i=1;i<=13;i++) {
			Board board = new Board();
			board.setMember(member1);
			board.setTitle(member1.getName() + "가 등록한 게시글 " + i);
			board.setContent(member1.getName() + "가 등록한 게시글 내용 " + i);
			boardRepo.save(board);
		}
		
		for(int i=1;i<=3;i++) {
			Board board = new Board();
			board.setMember(member2);
			board.setTitle(member2.getName() + "가 등록한 게시글 " + i);
			board.setContent(member2.getName() + "가 등록한 게시글 내용 " + i);
			boardRepo.save(board);
		}
	}
	
	//게시글 상세 조회 테스트
	public void testGetBoard() {
		Board board = boardRepo.findById(1L).get();
		
		System.out.println("[ " + board.getSeq() + "번 게시글 상세 정보 ]");
		System.out.println("제목\t : " + board.getTitle());
		System.out.println("작성자\t : " + board.getMember().getName());
		System.out.println("내용\t : " + board.getContent());
		System.out.println("등록일\t : " + board.getCreateDate());
		System.out.println("조회수\t : " + board.getCnt());
	}
	
	//특정 회원이 작성한 게시글 목록 조회 테스트
	public void testGetBoardList() {
		Member member = memberRepo.findById("member").get();
		
		System.out.println("[ " + member.getName() + "가 등록한 게시글 정보 ]");
		member.getBoardList().forEach(System.out::println);
	}
}
