package jpa.start;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
@Table(name = "MEMBER")
public class Member {
    @Id
    private String username;

    /*
        지연 로딩
        : 팀 엔티티를 실제 사용하는 시점에 JPA가 SQL을 호출하여 팀 엔티티를 조회
        ex) member.getTeam()
     */
    @ManyToOne(fetch = FetchType.LAZY)
    private Team team;
}
