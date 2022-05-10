package com.shop.shoppingmall.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "cart")
@Getter
@Setter
@ToString
public class Cart {

    @Id
    @Column(name = "cart_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne   // 회원 entity 와 1:1 매핑
    @JoinColumn(name = "member_id") // 외래키 지정
    private Member member;


}
