package com.example.recipe621.social.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("")
public class RootController {

    @RequestMapping("index")
    public String index() {
        return "index";
    }
}
