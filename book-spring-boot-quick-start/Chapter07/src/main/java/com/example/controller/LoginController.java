package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {
	
	@GetMapping("/login")
	public void login() {
	}
	
	@GetMapping("/loginSuccess")
	public void loginSuccess() {
	}
	
	@GetMapping("/accessDenied")
	public void accessDenied() {
	}
}
