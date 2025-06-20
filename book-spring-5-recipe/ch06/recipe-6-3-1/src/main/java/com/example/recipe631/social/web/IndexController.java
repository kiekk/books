package com.example.recipe631.social.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("")
public class IndexController {

    @RequestMapping("")
    public String root() {
        return "redirect:/index";
    }

    @RequestMapping("index")
    public String index() {
        return "index";
    }
}
