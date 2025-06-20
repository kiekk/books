package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

import com.example.domain.Member;
import com.example.service.MemberService;

/*
 * @SessionAttributes는 세션에 상태 정보를 저장할 때 사용합니다.
 * "member"로 설정했기 때문에 Model 객체 "member"라는 이름으로 저장된 데이터를 자동으로 세션에 등록합니다.
 */
@SessionAttributes("member")
@Controller
public class LoginController {
	
	@Autowired
	private MemberService memberService;
	
	@GetMapping("/login")
	public void loginView() {
	}
	
	//로그인 처리
	@PostMapping("/login")
	public String login(Member member, Model model) {
		Member findMember = memberService.getMember(member);
		
		if(findMember != null && findMember.getPassword().equals(member.getPassword())) {
			model.addAttribute("member", findMember);
			return "forward:getBoardList";
		} else {
			return "redirect:login";
		}
	}
	
	//로그아웃 처리
	@GetMapping("/logout")
	public String logout(SessionStatus status) {
		//세션을 강제로 종료
		status.setComplete();
		return "redirect:index.html";
	}
}
