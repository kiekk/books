package jpabook.start;

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
@Table(name = "LOCKER")
public class Locker {
    @Id
    @GeneratedValue
    @Column(name = "LOCKER_ID")
    private Long id;

    private String name;
}
