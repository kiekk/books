package com.AWSAndOauth2.book.springboot.domain.user;

import com.AWSAndOauth2.book.springboot.domain.posts.BaseTimeEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@Entity
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column
    private String pitcure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder
    public User(String name, String email, String picture, Role role){
        this.name = name;
        this.email = email;
        this.pitcure = picture;
        this.role = role;
    }

    public User update(String name, String picture){
        this.name = name;
        this.pitcure = picture;

        return this;
    }

    public String getRoleKey(){
        return this.role.getKey();
    }
}
