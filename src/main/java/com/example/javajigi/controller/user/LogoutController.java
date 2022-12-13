package com.example.javajigi.controller.user;

import com.example.javajigi.controller.Controller;
import com.example.javajigi.mvc.JspView;
import com.example.javajigi.mvc.View;
import com.example.javajigi.util.UserSessionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LogoutController implements Controller {

    @Override
    public View execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession();
        session.removeAttribute(UserSessionUtils.USER_SESSION_KEY);
        return new JspView("redirect:/home.do");
    }

}