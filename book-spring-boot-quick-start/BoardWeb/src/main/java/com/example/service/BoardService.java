package com.example.service;

import org.springframework.data.domain.Page;

import com.example.domain.Board;
import com.example.domain.Search;

public interface BoardService {
	
	//게시글 등록
	void insertBoard(Board board);
	
	//게시글 수정
	void updateBoard(Board board);
	
	//게시글 삭제
	void deleteBoard(Board board);
	
	//게시글 상세 조회
	Board getBoard(Board board);
	
	//게시글 목록 조회
	//Page<Board> getBoardList(Board Board);
	//검색 기능 추가
	Page<Board> getBoardList(Search search);
}
