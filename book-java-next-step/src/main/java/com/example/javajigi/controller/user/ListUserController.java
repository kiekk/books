package com.example.javajigi.controller.user;

import com.example.javajigi.controller.AbstractController;
import com.example.javajigi.dao.UserDao;
import com.example.javajigi.model.User;
import com.example.javajigi.mvc.ModelAndView;
import com.example.javajigi.util.UserSessionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class ListUserController extends AbstractController {

    private static final Logger log = LoggerFactory.getLogger(CreateUserController.class);

    @Override
    public ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!UserSessionUtils.isLogined(request.getSession())) {
            return jspView("redirect:/users/loginForm.do");
        }

        UserDao userDao = new UserDao();
        List<User> users = userDao.findAll();
        request.setAttribute("users", users);
        return jspView("/user/list.jsp").addObject("users", users);
    }

}