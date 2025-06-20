package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;

import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:appConfig.xml")
@Transactional
public class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    // 회원 가입 테스트
    @Test
    public void join() throws Exception {
        // Given
        Member member = new Member();
        member.setName("kim");

        // When
        Long saveId = memberService.join(member);

        // Then
        assertEquals(member, memberRepository.findOne(saveId));
    }
    
    // 중복 회원 테스트
    @Test(expected = IllegalStateException.class)
    public void duplicateException() throws Exception {
        // Given
        String name = "kim";
        
        Member member1 = new Member();
        member1.setName(name);

        Member member2 = new Member();
        member2.setName(name);
        
        // When
        memberService.join(member1);
        memberService.join(member2);
        
        // Then
        fail("예외가 발생해야 함");
    }
}
