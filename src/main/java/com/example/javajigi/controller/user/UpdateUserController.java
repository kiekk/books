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

public class UpdateUserController extends AbstractController {
    private static final Logger log = LoggerFactory.getLogger(UpdateUserController.class);

    @Override
    public ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        UserDao userDao = new UserDao();
        User user = userDao.findByUserId(request.getParameter("userId"));

        if (!UserSessionUtils.isSameUser(request.getSession(), user)) {
            throw new IllegalStateException("다른 사용자의 정보를 수정할 수 없습니다.");
        }

        User updateUser = new User(
                request.getParameter("userId"),
                request.getParameter("password"),
                request.getParameter("name"),
                request.getParameter("email"));
        log.debug("Update User : {}", updateUser);
        user.update(updateUser);
        return jspView("redirect:/home.do");
    }
}