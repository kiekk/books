package com.example.recipe413.court.web;

import com.example.recipe413.court.domain.Member;
import com.example.recipe413.court.domain.Members;
import com.example.recipe413.court.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestMemberController {

    private final MemberService memberService;

    @Autowired
    public RestMemberController(MemberService memberService) {
        super();
        this.memberService = memberService;
    }

    @RequestMapping("/members")
    @ResponseBody
    public Members getRestMembers() {
        Members members = new Members();
        members.addMembers(memberService.findAll());
        return members;
    }

    @RequestMapping("/member/{memberId}")
    @ResponseBody
    public Member getMember(@PathVariable("memberId") long memberID) {
        return memberService.find(memberID);
    }
}
