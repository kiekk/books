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

/*
    @Entity
    해당 클래스를 테이블과 매핑한다고 JPA에게 알려줍니다.
    @Entity가 적용된 클래스를 엔티티 클래스라고 부릅니다.

    @Table
    엔티티 클래스에 매핑할 테이블 정보를 알려줍니다.
    해당 어노테이션이 생략되면 클래스 이름을 테이블 이름으로 매핑합니다.

    @Id
    테이블의 기본 키를 매핑합니다.
    해당 어노테이션이 적용된 필드를 식별자 필드라고 합니다.

    @Column
    필드를 컬럼에 매핑하며, 생략될 경우 필드명으로 컬럼명과 매핑합니다.
 */
