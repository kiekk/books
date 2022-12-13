package com.example.javajigi.controller.user;

import com.example.javajigi.controller.Controller;
import com.example.javajigi.dao.UserDao;
import com.example.javajigi.model.User;
import com.example.javajigi.mvc.JspView;
import com.example.javajigi.mvc.View;
import com.example.javajigi.util.UserSessionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginController implements Controller {

    @Override
    public View execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String userId = request.getParameter("userId");
        String password = request.getParameter("password");

        UserDao userDao = new UserDao();
        User user = userDao.findByUserId(userId);

        if (user == null) {
            request.setAttribute("loginFailed", true);
            return new JspView("/user/login.jsp");
        }

        if (user.matchPassword(password)) {
            HttpSession session = request.getSession();
            session.setAttribute(UserSessionUtils.USER_SESSION_KEY, user);
            return new JspView("redirect:/home.do");
        } else {
            request.setAttribute("loginFailed", true);
            return new JspView("/user/login.jsp");
        }
    }

}