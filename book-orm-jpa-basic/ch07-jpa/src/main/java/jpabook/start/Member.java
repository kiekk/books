package jpabook.start;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;

@Getter
@Setter
@ToString
@NoArgsConstructor
// @Entity는 @Entity나 @MappedSuperclass로 지정한 클래스만 상속받을 수 있다.
@Entity
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "MEMBER_ID")),
        @AttributeOverride(name = "name", column = @Column(name = "MEMBER_NAME"))
})
public class Member extends BaseEntity {
    private String email;
}

/*
    @AttributeOverrides
    : 부모에게 물려받은 매핑 정보를 재정의
 */