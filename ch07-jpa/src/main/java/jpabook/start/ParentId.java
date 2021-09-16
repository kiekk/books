package jpabook.start;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ParentId implements Serializable {
    private String id1; // Parent.id1과 연결
    private String id2; // Parent.id2와 연결
}
/*
    식별자 클래스의 속성명과 엔티티에서 사용하는 식별자의 속성명이 같아야 합니다.
    Parent.id1 = ParentId.id1
    Parent.id2 = ParentId2.id2
 */
