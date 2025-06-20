package com.example.recipe732.board.web;

import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Repository("")
public class LoginController {

    @RequestMapping("login")
    public String login() {
        return "login";
    }

    @RequestMapping("logout-success")
    public String logoutSuccess() {
        return "logout-success";
    }
}