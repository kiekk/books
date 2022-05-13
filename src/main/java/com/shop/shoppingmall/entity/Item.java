package com.shop.shoppingmall.entity;

import com.shop.shoppingmall.dto.ItemFormDto;
import com.shop.shoppingmall.enums.ItemSellStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "item")
@Getter
@Setter
@ToString
public class Item {

    @Id
    @Column(name = "item_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(nullable = false)
    private int stockNumber;

    @Lob
    @Column(nullable = false)
    private String detail;

    @Enumerated(EnumType.STRING)
    private ItemSellStatus sellStatus;

    private LocalDateTime regTime;

    private LocalDateTime updateTime;

    public void updateItem(ItemFormDto itemFormDto) {
        this.name = itemFormDto.getName();
        this.price = itemFormDto.getPrice();
        this.stockNumber = itemFormDto.getStockNumber();
        this.detail = itemFormDto.getDetail();
        this.sellStatus = itemFormDto.getSellStatus();
    }

}
