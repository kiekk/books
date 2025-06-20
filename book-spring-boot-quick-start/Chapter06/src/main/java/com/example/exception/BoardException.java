package com.example.exception;

public class BoardException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	public BoardException(String message) {
		super(message);
	}
}
/*
 * 자바의 예외는 Checked Exception, Unchecked Exception으로 구분됩니다.
 * Checked Exception은 컴파일 시점에 발생하는 예외이고,
 * Unchecked Exception은 컴파일은 통과하지만 실행 시점에 발생하는 예외입니다.
 * 
 * 게시판 프로그램 실행 시에 발생할 수 있는 모든 예외를 다루기 위해 BoardException을 RuntimeException을 상속하여 구현합니다.
 */
