package com.example.config;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;

import com.example.domain.Member;

public class SecurityUser extends User {
	
	private static final long serialVersionUID = 1L;

	public SecurityUser(Member member) {
//		super(member.getId(), "{noop}" + member.getPassword(), 
//				AuthorityUtils.createAuthorityList(member.getRole().toString()));
		
		//PasswordEncoder로 비밀번호 암호화를 했기 때문에 회원의 비밀번호를 그대로 전달합니다.
		super(member.getId(), member.getPassword(), 
				AuthorityUtils.createAuthorityList(member.getRole().toString()));
	}
}
