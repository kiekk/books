package com.example.service;

import java.util.List;

import com.example.domain.BoardVO;

public interface BoardService {
	String hello(String name);
	
	BoardVO getBoard();
	
	List<BoardVO> getBoardList();
}
