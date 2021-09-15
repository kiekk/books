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
        Member 클래스에서는 team을 참조할 수 없다. (단방향)
     */
}
