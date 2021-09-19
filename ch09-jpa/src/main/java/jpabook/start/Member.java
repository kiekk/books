package jpabook.start;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

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

    // 근무 기간
    @Embedded
    Period workPeriod;

    // 집 주소
    @Embedded
    Address homeAddress;
}
