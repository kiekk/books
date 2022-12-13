package com.example.javajigi.controller.user;

import com.example.javajigi.controller.Controller;
import com.example.javajigi.dao.UserDao;
import com.example.javajigi.model.User;
import com.example.javajigi.mvc.JspView;
import com.example.javajigi.mvc.View;
import com.example.javajigi.util.UserSessionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class ListUserController implements Controller {

    private static final Logger log = LoggerFactory.getLogger(CreateUserController.class);

    @Override
    public View execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!UserSessionUtils.isLogined(request.getSession())) {
            return new JspView("redirect:/users/loginForm.do");
        }

        UserDao userDao = new UserDao();
        List<User> users = userDao.findAll();
        request.setAttribute("users", users);
        return new JspView("/user/list.jsp");
    }

}