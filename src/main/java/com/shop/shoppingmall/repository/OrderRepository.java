package com.shop.shoppingmall.repository;

import com.shop.shoppingmall.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
