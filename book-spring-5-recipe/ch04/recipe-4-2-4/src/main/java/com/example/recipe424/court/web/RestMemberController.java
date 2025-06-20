package com.example.recipe424.court.web;

import com.example.recipe424.court.domain.Member;
import com.example.recipe424.court.domain.Members;
import com.example.recipe424.court.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class RestMemberController {

    private final MemberService memberService;

    @Autowired
    public RestMemberController(MemberService memberService) {
        super();
        this.memberService = memberService;
    }

    @RequestMapping("/members")
    @ResponseBody
    public ResponseEntity<?> getRestMembers() {
        Members members = new Members();
        members.addMembers(memberService.findAll());
        return new ResponseEntity<>(members, HttpStatus.OK);
    }

    @RequestMapping("/member/{memberId}")
    @ResponseBody
    public ResponseEntity<?> getMember(@PathVariable("memberId") long memberID) {
        Member member = memberService.find(memberID);
        if (member != null) {
            return new ResponseEntity<>(member, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

}
