package com.shop.shoppingmall.controller;

import com.shop.shoppingmall.dto.MemberFormDto;
import com.shop.shoppingmall.entity.Member;
import com.shop.shoppingmall.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("new")
    public String memberForm(Model model) {
        MemberFormDto memberFormDto = new MemberFormDto();

        model.addAttribute("memberFormDto", memberFormDto);
        return "member/memberForm";
    }

    @PostMapping("new")
    public String memberForm(MemberFormDto memberFormDto) {
        Member member = Member.createMember(memberFormDto, passwordEncoder);
        memberService.saveMember(member);

        return "redirect:/";
    }

}
