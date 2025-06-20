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
@IdClass(MemberProductId.class)
public class MemberProduct {

    @Id
    @ManyToOne
    @JoinColumn(name = "MEMBER_ID")
    private Member member;  // MemberProductId.member와 연결

    @Id
    @ManyToOne
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;    // MemberProductId.product와 연결

    private int orderAmount;
}

/*
    @IdClass : 복합 기본키 매핑
    복합 키를 사용하기 위해선 별도의 식별자 클래스가 필요 -> MemberProductId.class
    
    식별자 클래스 특징
    1. 복합 키는 별도의 식별자 클래스로 만들어야 함
    2. Serializable을 구현해야 함
    3. equals와 hashCode 메소드를 구현해야 함
    4. 기본 생성자가 있어야 함
    5. 식별자 클래스는 public이어야 함
    6. @IdClass를 사용하는 방법 외에 @EmbeddedId를 사용해도 무방
 */