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
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "DTYPE")
public abstract class Item {
    @Id
    @GeneratedValue
    @Column(name = "ITEM_ID")
    private Long id;

    private String name;
    private int price;
}
/*
    @Inheritance(strategy = InheritanceType.JOINED)
    : 상속 매핑 중 조인 전략 사용
    
    @DiscriminatorColumn(name = "DTYPE")
    : 부모 클래스에 구분 컬럼을 지정, 이 컬럼으로 자식 테이블을 구분
    
    @DiscriminatorValue("B")
    : 엔티티를 저장할 때 구분 컬럼에 입력할 값을 지정, 이 값으로 자식 테이블이 구분 됨
 */
