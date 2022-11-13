package com.example.studyspringwebflow.repository;

import com.example.studyspringwebflow.entity.Account;
import com.example.studyspringwebflow.entity.Order;

import java.util.List;

public interface OrderRepository {

    Order findById(long id);

    Order save(Order order);

    List<Order> findByAccount(Account account);

}
