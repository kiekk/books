package jpabook.start;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@NoArgsConstructor
@Table(name = "TEAM")
public class Team {
    @Id
    @GeneratedValue
    @Column(name = "TEAM_ID")
    private Long id;

    private String name;
    
    @OneToMany
    @JoinColumn(name = "TEAM_ID")   // MEMBER 테이블의 TEAM_ID(FK)
    private List<Member> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }
}
