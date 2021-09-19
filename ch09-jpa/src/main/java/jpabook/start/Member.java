package jpabook.start;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class Member {
    @Id
    @GeneratedValue
    private Long id;

    /*
        기본 값 타입: String, age
     */
    private String name;
    private int age;
}
