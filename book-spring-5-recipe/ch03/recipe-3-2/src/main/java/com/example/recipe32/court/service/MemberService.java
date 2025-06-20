package com.example.recipe32.court.service;

import com.example.recipe32.court.domain.Member;

import java.util.List;

public interface MemberService {

    void add(Member member);

    void remove(String memberName);

    List<Member> list();
}
