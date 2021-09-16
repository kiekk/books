package jpabook.start;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Entity
// 식별자 클래스 ParentId를 이용해 복합키 구성
@IdClass(ParentId.class)
public class Parent {

    @Id
    @Column(name = "PARENT_ID1")
    private String id1;  // ParentId.id1과 연결

    @Id
    @Column(name = "PARENT_ID2")
    private String id2; // ParentId.id2와 연결

    private String name;
}
