package com.shop.shoppingmall.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MainItemDto {

    private Long id;
    private String name;
    private String itemDetail;
    private String imgUrl;
    private Integer price;

    @QueryProjection
    public MainItemDto(Long id, String name, String itemDetail, String imgUrl, Integer price) {
        this.id = id;
        this.name = name;
        this.itemDetail = itemDetail;
        this.imgUrl = imgUrl;
        this.price = price;
    }
}
