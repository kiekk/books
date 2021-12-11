package com.example.recipe422.court.web;

import com.example.recipe422.court.domain.Members;
import com.example.recipe422.court.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RestMemberController {

    private final MemberService memberService;

    @Autowired
    public RestMemberController(MemberService memberService) {
        super();
        this.memberService = memberService;
    }

    @RequestMapping(value="/members.xml", produces=MediaType.APPLICATION_XML_VALUE)
    public String getRestMembersXml(Model model) {
        Members members = new Members();
        members.addMembers(memberService.findAll());
        model.addAttribute("members", members);
        return "xmlmembertemplate";
    }

    @RequestMapping(value="/members.json", produces= MediaType.APPLICATION_JSON_VALUE)
    public String getRestMembersJson(Model model) {
        Members members = new Members();
        members.addMembers(memberService.findAll());
        model.addAttribute("members", members);
        return "jsonmembertemplate";
    }
}
