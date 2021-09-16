package jpabook.start;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@Getter
@Setter
@ToString
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
}

/*
    @MappedSuperclass
    : 실제 테이블과 매핑되지 않음
    : 엔티티가 아니므로 em.find()나 JPQL에서 사용할 수 없음
 */
