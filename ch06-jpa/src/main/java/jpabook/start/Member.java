package jpabook.start;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
@Table(name = "MEMBER")
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "MEMBER_ID")
    private Long id;

    private String username;

    /*
        일대다 양방향 관계 설정
        다대일 쪽을 읽기 전용(insertable, updatable = false)으로 만들어줌
        이럴바에는 다대일 양방향 매핑을 사용하자.
     */
    @ManyToOne
    @JoinColumn(name = "TEAM_Id", insertable = false, updatable = false)
    private Team team;

    public Member(String username) {
        this.username = username;
    }

    /*
        Member 클래스에서는 team을 참조할 수 없다. (단방향)
     */
}
