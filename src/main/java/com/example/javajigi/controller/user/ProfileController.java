package com.example.javajigi.controller.user;

import com.example.javajigi.controller.Controller;
import com.example.javajigi.dao.UserDao;
import com.example.javajigi.db.DataBase;
import com.example.javajigi.model.User;
import com.example.javajigi.mvc.JspView;
import com.example.javajigi.mvc.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProfileController implements Controller {

    @Override
    public View execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String userId = request.getParameter("userId");
        UserDao userDao = new UserDao();
        User user = userDao.findByUserId(userId);

        if (user == null) {
            throw new NullPointerException("사용자를 찾을 수 없습니다.");
        }
        request.setAttribute("user", user);
        return new JspView("/user/profile.jsp");
    }

}