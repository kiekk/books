package com.shop.shoppingmall.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemDto {

    private String name;

    private int count;

    private int orderPrice;

    private String imgUrl;

    public OrderItemDto(String name, int count, int orderPrice, String imgUrl) {
        this.name = name;
        this.count = count;
        this.orderPrice = orderPrice;
        this.imgUrl = imgUrl;
    }
}
