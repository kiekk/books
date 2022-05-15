package com.shop.shoppingmall.service;

import com.shop.shoppingmall.dto.OrderDto;
import com.shop.shoppingmall.entity.Item;
import com.shop.shoppingmall.entity.Member;
import com.shop.shoppingmall.entity.Order;
import com.shop.shoppingmall.entity.OrderItem;
import com.shop.shoppingmall.repository.ItemRepository;
import com.shop.shoppingmall.repository.MemberRepository;
import com.shop.shoppingmall.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final ItemRepository itemRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;

    public Long order(OrderDto orderDto, String email) {
        Item item = itemRepository.findById(orderDto.getItemId()).orElseThrow(EntityNotFoundException::new);

        Member member = memberRepository.findByEmail(email);

        List<OrderItem> orderItemList = new ArrayList<>();
        OrderItem orderItem = OrderItem.createOrderItem(item, orderDto.getCount());
        orderItemList.add(orderItem);

        Order order = Order.createOrder(member, orderItemList);
        orderRepository.save(order);

        return order.getId();
    }
}
