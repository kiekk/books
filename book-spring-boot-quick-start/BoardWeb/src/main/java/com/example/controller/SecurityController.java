package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SecurityController {
	
	@GetMapping("/system/login")
	public void login() {
	}
	
	@GetMapping("/system/accessDenied")
	public void accessDenied() {
	}
	
	@GetMapping("/system/logout")
	public void logout() {
	}
	
	@GetMapping("/admin/adminPage")
	public void adming() {
	}
}
