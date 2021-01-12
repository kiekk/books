package com.example.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude="boardList")
@Entity
public class Member {
	@Id
	@Column(name="MEMBER_ID")
	private String id;
	private String password;
	private String name;
	private String role;
	
	//회원에서 게시판을 조회하기 위해 관계 설정
	//일대다 관계이기 때문에 List를 사용해야합니다.
	/*
	 * fetch : 연관관계에 있는 게시글 정보도 같이 조회할 것인지를 설정합니다.
	 * 기본 값은 LAZY이며, EAGER로 설정했기 때문에 회원 정보를 가져올 때 게시글 목록도 같이 조회될 것입니다.
	 */
	//@OneToMany(mappedBy="member", fetch=FetchType.EAGER)
	@OneToMany(mappedBy="member", fetch=FetchType.EAGER, cascade=CascadeType.ALL)
	//cascade속성은 영속성 전이를 설정합니다.
	//CascadeType.ALL은 회원 객체가 영속화,수정,삭제 될 때 회원과 관련된 게시판도 같이 변경될 것입니다.
	private List<Board> boardList = new ArrayList<>();
}
