package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.domain.Board;
import com.example.domain.QBoard;
import com.example.domain.Search;
import com.example.persistence.BoardRepository;
import com.querydsl.core.BooleanBuilder;

@Service
public class BoardServiceImpl implements BoardService {

	@Autowired
	private BoardRepository boardRepo;
	
	@Override
	public void insertBoard(Board board) {
		boardRepo.save(board);
	}

	@Override
	public void updateBoard(Board board) {
		Board findBoard = boardRepo.findById(board.getSeq()).get();
		
		findBoard.setTitle(board.getTitle());
		findBoard.setContent(board.getContent());
		boardRepo.save(findBoard);
	}

	@Override
	public void deleteBoard(Board board) {
		boardRepo.deleteById(board.getSeq());
	}

	@Override
	public Board getBoard(Board board) {
		return boardRepo.findById(board.getSeq()).get();
	}

//	@Override
//	public Page<Board> getBoardList(Board Board) {
//		Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "seq");
//		return boardRepo.getBoardList(pageable);
//	}
	
	//검색 기능 추가
	@Override
	public Page<Board> getBoardList(Search search) {
		BooleanBuilder builder = new BooleanBuilder();
		
		QBoard qboard = QBoard.board;
		if(search.getSearchCondition().equals("TITLE"))
			builder.and(qboard.title.like("%" + search.getSearchKeyword() + "%"));
		else if(search.getSearchCondition().equals("CONTENT"))
			builder.and(qboard.content.like("%" + search.getSearchKeyword() + "%"));
		
		Pageable pageable = PageRequest.of(0, 10, Sort.Direction.DESC, "seq");
		return boardRepo.findAll(builder, pageable);
	}
}
