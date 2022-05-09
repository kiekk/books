package com.shop.shoppingmall.controller;

import com.shop.shoppingmall.dto.ItemDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @GetMapping("ex03")
    public String thymeleafExample03(Model model) {
        List<ItemDto> itemDtoList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ItemDto itemDto = new ItemDto();
            itemDto.setDetail("상품 상세 설명_" + i);
            itemDto.setName("테스트 상품_" + i);
            itemDto.setPrice(1000 * i);
            itemDto.setRegTime(LocalDateTime.now());

            itemDtoList.add(itemDto);
        }

        model.addAttribute("itemDtoList", itemDtoList);
        return "thymeleafEx/thymeleafEx03";
    }

    @GetMapping("ex04")
    public String thymeleafExample04(Model model) {
        List<ItemDto> itemDtoList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            ItemDto itemDto = new ItemDto();
            itemDto.setDetail("상품 상세 설명_" + i);
            itemDto.setName("테스트 상품_" + i);
            itemDto.setPrice(1000 * i);
            itemDto.setRegTime(LocalDateTime.now());

            itemDtoList.add(itemDto);
        }

        model.addAttribute("itemDtoList", itemDtoList);
        return "thymeleafEx/thymeleafEx04";
    }
}
