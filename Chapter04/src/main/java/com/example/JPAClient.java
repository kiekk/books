package com.example;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import com.example.domain.Board;

public class JPAClient {
	//글 등록
//	public static void main(String[] args) {
//		//EntityManager 생성
//		EntityManagerFactory emf = Persistence.createEntityManagerFactory("Chapter04");
//		EntityManager em = emf.createEntityManager();
//		
//		//Transaction 생성
//		EntityTransaction tx = em.getTransaction();
//		try {
//			//Transaction 시작
//			tx.begin();
//			
//			Board board = new Board();
//			board.setTitle("JPA 제목");
//			board.setWriter("관리자");
//			board.setContent("JPA 내용입니다.");
//			board.setCreateDate(new Date());
//			board.setCnt(0L);
//			
//			//글 등록
//			em.persist(board);
//			
//			//Transaction commit
//			tx.commit();
//		} catch (Exception e) {
//			e.printStackTrace();
//			//Transaction rollback
//			tx.rollback();
//		} finally {
//			em.close();
//			emf.close();
//		}
//	}
	
	//글 조회
	public static void main(String[] args) {
		//EntityManager 생성
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("Chapter04");
		EntityManager em = emf.createEntityManager();
		
		try {
			//글 상세 조회
			Board searchBoard = em.find(Board.class, 1L);
			//만약 해당하는 게시글이 없을 경우엔 NullPointerException 발생
			System.out.println("---> " + searchBoard.toString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			em.close();
			emf.close();
		}
	}
}
