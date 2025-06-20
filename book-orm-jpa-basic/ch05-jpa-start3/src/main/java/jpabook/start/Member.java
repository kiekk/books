package jpabook.start;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@ToString
@Entity
@Table(name = "MEMBER")
public class Member {
    @Id
    @Column(name = "MEMBER_ID")
    private String id;
    private String username;

    @ManyToOne
    @JoinColumn(name = "TEAM_ID")
    private Team team;

    public Member(String id, String username) {
        this.id = id;
        this.username = username;
    }

    // 연관관계 편의 메소드
    public void setTeam(Team team) {
        // 기존 팀이 있을 경우 기존 팀과 관계 제거
        if(this.team != null) {
            this.team.getMembers().remove(this);
        }
        this.team = team;
        team.getMembers().add(this);
    }
    /*
        관게를 변경하고 영속성 컨텍스트가 아직 살아있는 상태에서
        getMembers()를 호출하면 이전 member가 반환된다.
     */
}
