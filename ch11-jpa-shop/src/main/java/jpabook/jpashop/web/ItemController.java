package jpabook.jpashop.web;

import jpabook.jpashop.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class ItemController {

    @Autowired
    ItemService itemService;
}
