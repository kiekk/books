package com.shop.shoppingmall.entity;

import com.shop.shoppingmall.enums.ItemSellStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class Item {

    private Long id;
    private String name;
    private int price;
    private String stockNumber;
    private String detail;
    private ItemSellStatus sellStatus;
    private LocalDateTime regTime;
    private LocalDateTime updateTime;

}
