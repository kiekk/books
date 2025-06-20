package com.example.recipe422.court.service;

import com.example.recipe422.court.domain.Member;

import java.util.Collection;

public interface MemberService {

    Collection<Member> findAll();
    Member find(long id);
}
