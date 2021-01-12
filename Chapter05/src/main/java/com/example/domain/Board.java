package com.example.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude="member")
//exclude 속성을 사용하지 않으면 Member와 Board의 양방향 관계에서
//서로 toString()을 호출하는 순환 참조에 빠지게 됩니다.
@Entity
public class Board {
	@Id
	@GeneratedValue
	private Long seq;
	private String title;
	//Member 객체와 연관 관계를 맺기위해 기존의 필드는 주석 처리
	//private String writer;
	private String content;
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date createDate;
	private Long cnt;
	
	@ManyToOne //다대일 관계 설정
	@JoinColumn(name="MEMBER_ID", nullable=false)
	/*
	 * nullable을 설정하지 않으면 기본 값이 true로 설정되는데,
	 * 그렇게 되면 Member 객체와 outer join하기 때문에 성능상 좋지 않습니다.
	 * nullable을 false로 설정하게 되면 outer join이 아닌 inner join합니다.
	 */
	private Member member;
	
	//Board에 회원 객체를 설정할 때, 회원이 소유한 게시글도 일반 자바 객체에서도 사용할 수 있도록 저장합니다.
	public void setMember(Member member) {
		this.member = member;
		member.getBoardList().add(this);
	}
}
