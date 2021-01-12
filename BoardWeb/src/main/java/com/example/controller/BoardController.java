package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.board.security.SecurityUser;
import com.example.domain.Board;
import com.example.domain.Search;
import com.example.service.BoardService;

@Controller
@RequestMapping("/board/")
public class BoardController {
	@Autowired
	private BoardService boardService;
	
	@RequestMapping("/getBoardList")
	public String getBoardList(Model model, Search search) {
		
		if(search.getSearchCondition() == null) 
			search.setSearchCondition("TITLE");
		
		if(search.getSearchKeyword() == null)
			search.setSearchKeyword("");
		
		Page<Board> boardList = boardService.getBoardList(search);
		model.addAttribute("boardList", boardList);
		return "board/getBoardList";
	}
	
	@GetMapping("/getBoard")
	public String getBoard(Model model, Board board) {
		model.addAttribute("board", boardService.getBoard(board));
		return "board/getBoard";
	}
	
	@GetMapping("/insertBoard")
	public String isnertBoardView() {
		return "board/insertBoard";
	}
	
	/*
	 * 로그인 한 유저만 게시글 기능을 이용할 수 있기 때문에, 로그인 한 유저 개겣를 가지고 있는 SecurityUser 객체를 매개변수로 받아야 합니다.
	 * 이때 SecurityUser에 @@AuthenticationPrincipal 어노테이션을 추가해야만 인증 정보를 가지고 있는 SecurityUser 객체가 할당됩니다.
	 * 그리고 SecurityUser 객체에서 Member 엔티티를 꺼내 Board에 셋팅하면 됩니다.
	 */
	@PostMapping("/insertBoard")
	public String insertBoard(Board board, @AuthenticationPrincipal SecurityUser principal) {
		board.setMember(principal.getMember());
		boardService.insertBoard(board);
		return "redirect:getBoardList";
	}
	
	@PostMapping("/updateBoard")
	public String upadteBoard(Board board) {
		boardService.updateBoard(board);
		return "redirect:getBoardList";
	}
	
	@GetMapping("/deleteBoard")
	public String deleteBoard(Board board) {
		boardService.deleteBoard(board);
		return "redirect:getBoardList";
	}
}
