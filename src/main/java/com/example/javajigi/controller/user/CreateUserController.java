package com.example.javajigi.controller.user;

import com.example.javajigi.controller.Controller;
import com.example.javajigi.dao.UserDao;
import com.example.javajigi.model.User;
import com.example.javajigi.mvc.JspView;
import com.example.javajigi.mvc.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.SQLException;

public class CreateUserController implements Controller {

    private static final Logger log = LoggerFactory.getLogger(CreateUserController.class);

    @Override
    public View execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User user = new User(
                request.getParameter("userId"),
                request.getParameter("password"),
                request.getParameter("name"),
                request.getParameter("email")
        );
        log.debug("User : {}", user);

        UserDao userDao = new UserDao();
        try {
            userDao.insert(user);
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
        return new JspView("redirect:/users/list.do");
    }
}
