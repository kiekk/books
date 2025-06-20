package com.example.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

import com.example.domain.Board;
import com.example.domain.Member;
import com.example.service.BoardService;

@SessionAttributes("member")
@Controller
public class BoardController {
	
	@Autowired
	private BoardService boardService;
	/*
	 	1. 컨트롤러에서 임의로 데이터를 생성하여 View 페이지로 데이터를 전달
	@RequestMapping("/getBoardList")
	public String getBoardList(Model model) {
		List<Board> boardList = new ArrayList<>();
		
		for(int i=1;i<=10;i++) {
			Board board = new Board();
			board.setSeq(new Long(i));
			board.setTitle("게시판 프로그램 예제");
			board.setWriter("apple");
			board.setContent("게시판 프로그램 예제 내용입니다.");
			board.setCreateDate(new Date());
			board.setCnt(0L);
			boardList.add(board);
		}
		model.addAttribute("boardList", boardList);
		return "getBoardList";
	}
	 */
	
	//2. JPA를 사용하여 실제 테이블에 저장되어 있는 데이터를 View 페이지로 전달
//	@RequestMapping("/getBoardList")
//	public String getBoardList(Model model, Board board) {
//		List<Board> boardList = boardService.getBoardList(board);
//		
//		model.addAttribute("boardList", boardList);
//		
//		return "getBoardList";
//	}
	
	//게시글 등록 페이지 이동
	@GetMapping("/insertBoard")
	public String insertBoardView(@ModelAttribute("member") Member member) {
		//session이 비어있다면 login페이지로 return
		if(member.getId() == null) {
			return "redirect:login";
		}
		
		return "insertBoard";
	}
	
	//게시글 등록 처리
	@PostMapping("/insertBoard")
	public String insertBoard(@ModelAttribute("member") Member member, Board board) {
		//session이 비어있다면 login페이지로 return
		if(member.getId() == null) {
			return "redirect:login";
		}
		
		boardService.insertBoard(board);
		return "redirect:getBoardList";
	}
	
	//게시글 상세 조회
	@GetMapping("/getBoard")
	public String getBoard(@ModelAttribute("member") Member member, Board board, Model model) {
		//session이 비어있다면 login페이지로 return
		if(member.getId() == null) {
			return "redirect:login";
		}
		
		model.addAttribute("board", boardService.getBoard(board));
		return "getBoard";
	}
	
	//게시글 수정
	@PostMapping("/updateBoard")
	public String updateBoard(@ModelAttribute("member") Member member, Board board) {
		//session이 비어있다면 login페이지로 return
		if(member.getId() == null) {
			return "redirect:login";
		}
		
		boardService.updateBoard(board);
		return "forward:getBoardList";
	}
	
	//게시글 삭제
	@GetMapping("/deleteBoard")
	public String deleteBoard(@ModelAttribute("member") Member member, Board board) {
		//session이 비어있다면 login페이지로 return
		if(member.getId() == null) {
			return "redirect:login";
		}
		
		boardService.deleteBoard(board);
		return "forward:getBoardList";
	}
	
	//타임리프를 이용하여 브라우저에 출력
	@GetMapping("/hello")
	public void hello(Model model) {
		model.addAttribute("greeting", "Hello Thymeleaf");
	}
	
	//로그인 성공한 사용자만 게시판 기능을 사용할 수 있도록 제어
	@ModelAttribute("member")
	public Member setMember() {
		return new Member();
	}
	
	//기존의 getBoardList() 메소드에서 로그인 확인기능 추가
	@RequestMapping("/getBoardList")
	public String getBoardList(@ModelAttribute("member") Member member, Model model, Board board) {
		//session이 비어있다면 login페이지로 return
		if(member.getId() == null) {
			return "redirect:login";
		}
		
		List<Board> boardList = boardService.getBoardList(board);
		
		model.addAttribute("boardList", boardList);
		return "getBoardList";
	}
}
