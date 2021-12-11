package com.example.recipe412.court.service;

import com.example.recipe412.court.domain.Member;

import java.util.Collection;


public interface MemberService {

    Collection<Member> findAll();
    Member find(long id);
}
