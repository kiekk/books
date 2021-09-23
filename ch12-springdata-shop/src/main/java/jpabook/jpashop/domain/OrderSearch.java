package jpabook.jpashop.domain;

import jpabook.jpashop.enums.OrderStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.Specification;

import static jpabook.jpashop.domain.OrderSpec.memberNameLike;
import static jpabook.jpashop.domain.OrderSpec.orderStatusEq;
import static org.springframework.data.jpa.domain.Specifications.where;

@Getter
@Setter
public class OrderSearch {
    private String memberName;  // 회원 이름
    private OrderStatus orderStatus;    // 주문 상태 [READY, CANCEL]

    public Specification<Order> toSpecification() {
        return where(memberNameLike(memberName)).and(orderStatusEq(orderStatus));
    }
}
