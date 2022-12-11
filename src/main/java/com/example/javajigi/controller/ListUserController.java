package com.example.javajigi.controller;

import com.example.javajigi.db.DataBase;
import com.example.javajigi.util.UserSessionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ListUserController implements Controller {

    @Override
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!UserSessionUtils.isLogined(request.getSession())) {
            return "redirect:/users/loginForm.do";
        }

        request.setAttribute("users", DataBase.findAll());
        return "/user/list.jsp";
    }

}