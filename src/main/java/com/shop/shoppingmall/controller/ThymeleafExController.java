package com.shop.shoppingmall.controller;

import com.shop.shoppingmall.dto.ItemDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/thymeleaf")
public class ThymeleafExController {

    @GetMapping("ex02")
    public String thymeleafExample02(ItemDto itemDto) {
        itemDto.setDetail("상품 상세 설명");
        itemDto.setName("테스트 상품 1");
        itemDto.setPrice(10000);
        itemDto.setRegTime(LocalDateTime.now());

        return "thymeleafEx/thymeleafEx02";
    }
}
