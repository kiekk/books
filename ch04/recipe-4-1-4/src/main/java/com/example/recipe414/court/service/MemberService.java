package com.example.recipe414.court.service;

import com.example.recipe414.court.domain.Member;

import java.util.Collection;

public interface MemberService {

    Collection<Member> findAll();
    Member find(long id);
}
