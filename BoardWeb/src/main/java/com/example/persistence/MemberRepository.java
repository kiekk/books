package com.example.persistence;

import org.springframework.data.repository.CrudRepository;

import com.example.domain.Member;

public interface MemberRepository extends CrudRepository<Member, String> {

}
