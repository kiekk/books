package jpabook.start;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@ToString
@Getter
@Setter
@Entity
@Table(name = "MEMBER")
public class Member {
    @Id
    @Column(name = "ID")
    private String id;
    
    @Column(name = "NAME")
    private String username;
    
    // mapping이 없는 field
    // field명으로 column을 매핑
    private Integer age;
}
