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
public class GrandChild {

    @Id
    @GeneratedValue
    @Column(name = "GRANDCHILD_ID")
    private Long id;

    private String name;

    @ManyToOne
    @JoinColumn(name = "CHILD_ID")
    private Child child;
}
