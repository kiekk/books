package com.example.recipe32.court.web;

import com.example.recipe32.court.domain.Member;
import com.example.recipe32.court.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MemberController {

    private MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @RequestMapping("/member/add")
    public String addMember(Model model) {
        model.addAttribute("member", new Member());
        model.addAttribute("guests", memberService.list());
        return "memberList";
    }


    // 메서드를 /member/remove, /member/delete 두 URL에 매핑합니다.
    @RequestMapping(value = {"/member/remove", "/member/delete"}, method = RequestMethod.GET)
    public String removeMember(@RequestParam("memberName")
                                       String memberName) {
        memberService.remove(memberName);

        return "redirect:";
    }
}
