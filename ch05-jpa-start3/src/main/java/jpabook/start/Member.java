package jpabook.start;

import lombok.*;

@Getter
@Setter
@ToString
public class Member {
    private String id;
    private String username;
    private Team team;

    public Member(String id, String username) {
        this.id = id;
        this.username = username;
    }
}
