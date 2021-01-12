package com.example.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.domain.Member;
import com.example.persistence.MemberRepository;

@Service
public class BoardUserDetailsService implements UserDetailsService {
	@Autowired
	private MemberRepository memberRepo;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		//MemberRepository로 회원 정보 조회한 후
		//UserDetails 타입의 객체로 반환.
		Optional<Member> optional = memberRepo.findById(username);
		if(!optional.isPresent()) {
			throw new UsernameNotFoundException(username + " 사용자 없음");
		}else {
			Member member = optional.get();
			return new SecurityUser(member);
		}
		
		
	}
}
