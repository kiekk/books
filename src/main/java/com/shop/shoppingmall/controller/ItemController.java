package com.shop.shoppingmall.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ItemController {

    @RequestMapping("/admin/item/new")
    public String itemForm() {
        return "item/itemForm";
    }
}
