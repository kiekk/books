package com.shop.shoppingmall.repository;

import com.shop.shoppingmall.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Member findByEmail(String email);
    
}
