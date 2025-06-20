package jpabook.jpashop.repository.custom;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderSearch;

import java.util.List;

public interface CustomOrderRepository {
    List<Order> search(OrderSearch orderSearch);
}
