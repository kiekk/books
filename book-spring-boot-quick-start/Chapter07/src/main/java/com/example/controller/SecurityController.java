package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SecurityController {
	/*
	 * '/' : 인증을 하지 않은 모든 사용자가 접근할 수 있습니다.
	 * '/member' : 인증을 통과한 사용자만 접근할 수 있습니다.
	 * '/manager' : 인증을 통과했고, Manager 권한을 가진 사용자만 접근할 수 있습니다.
	 * '/admin' : 인증을 통과했고, Admin 권한을 가진 사용자만 접근할 수 있습니다.
	 */
	@GetMapping("/")
	public String index() {
		System.out.println("index 요청입니다.");
		return "index";
	}
	
	@GetMapping("/member")
	public void forMember() {
		System.out.println("Member 요청입니다.");
	}
	
	@GetMapping("/manager")
	public void forManager() {
		System.out.println("Manager 요청입니다.");
	}
	
	@GetMapping("/admin")
	public void forAdmin() {
		System.out.println("Admin 요청입니다.");
	}
}
