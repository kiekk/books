package com.example;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import com.example.domain.Board;
/*
 * EntityManager 객체가 CRUD 기능의 메소드들을 제공하는데,
 * EntityManagerFactory로부터 EntityManager 객체를 생성해야 합니다.
 * 
 * EntityManager가 제공하는 메소드
 * persist() => INSERT
 * merge() => UPDATE
 * remove() => DELETE
 * find() => SELECT ONE
 * createQuery() => SELECT LIST
 */
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
//	public static void main(String[] args) {
//		//EntityManager 생성
//		EntityManagerFactory emf = Persistence.createEntityManagerFactory("Chapter04");
//		EntityManager em = emf.createEntityManager();
//		
//		try {
//			//글 상세 조회
//			Board searchBoard = em.find(Board.class, 1L);
//			//만약 해당하는 게시글이 없을 경우엔 NullPointerException 발생
//			System.out.println("---> " + searchBoard.toString());
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			em.close();
//			emf.close();
//		}
//	}
	
	//글 수정
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
//			Board board = em.find(Board.class, 1L);
//			board.setTitle("검색한 게시글의 수정 제목");
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
	
	//글 삭제
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
//			//삭제할 게시글 조회
//			Board board = em.find(Board.class, 1L);
//			board.setSeq(1L);
//		
//			//게시글 삭제
//			em.remove(board);
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
	
	//목록 검색, JPQL
	public static void main(String[] args) {
		//EntityManager 생성
		EntityManagerFactory emf = Persistence.createEntityManagerFactory("Chapter04");
		EntityManager em = emf.createEntityManager();
		
		//Transaction 생성
		EntityTransaction tx = em.getTransaction();
		try {
			//Transaction 시작
			tx.begin();
			
			//글 목록 조회
			String jpql = "select b from Board b order by b.seq desc";
			List<Board> boardList = em.createQuery(jpql, Board.class).getResultList();
			
			boardList.forEach(System.out::println);
			
			//Transaction commit
			tx.commit();
		} catch (Exception e) {
			e.printStackTrace();
			//Transaction rollback
			tx.rollback();
		} finally {
			em.close();
			emf.close();
		}
	}
}
