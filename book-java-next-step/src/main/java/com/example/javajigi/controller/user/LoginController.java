package com.example.javajigi.controller.user;

import com.example.javajigi.controller.AbstractController;
import com.example.javajigi.dao.UserDao;
import com.example.javajigi.model.User;
import com.example.javajigi.mvc.ModelAndView;
import com.example.javajigi.util.UserSessionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginController extends AbstractController {

    @Override
    public ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String userId = request.getParameter("userId");
        String password = request.getParameter("password");

        UserDao userDao = new UserDao();
        User user = userDao.findByUserId(userId);

        if (user == null) {
            return jspView("/user/login.jsp").addObject("loginFailed", true);
        }

        if (user.matchPassword(password)) {
            HttpSession session = request.getSession();
            session.setAttribute(UserSessionUtils.USER_SESSION_KEY, user);
            return jspView("redirect:/home.do");
        } else {
            return jspView("/user/login.jsp").addObject("loginFailed", true);
        }
    }

}