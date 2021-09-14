package jpabook.start;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class Member {
    private String id;
    private String username;
    private Integer age;
}
