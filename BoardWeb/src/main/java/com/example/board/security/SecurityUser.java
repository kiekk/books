package com.example.board.security;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

import com.example.domain.Member;

public class SecurityUser extends User {

	private static final long serialVersionUID = 1L;
	private Member member;
	
	public SecurityUser(Member member) {
		super(member.getId(), member.getPassword(),
				AuthorityUtils.createAuthorityList(member.getRole().toString()));
		this.member = member;
	}
	
	public Member getMember() {
		return member;
	}
}
