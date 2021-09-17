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
        즉시 로딩
        : 회원 엔티티를 조회할 때 팀 엔티티도 함께 조회
     */
    @ManyToOne(fetch = FetchType.EAGER)
    private Team team;
}
