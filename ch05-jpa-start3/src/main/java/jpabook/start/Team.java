package jpabook.start;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Entity
@Table(name = "TEAM")
@NoArgsConstructor
public class Team {
    @Id
    @Column(name = "TEAM_ID")
    private String id;

    private String name;
}
