package com.example.javajigi.controller.user;

import com.example.javajigi.controller.Controller;
import com.example.javajigi.dao.QuestionDao;
import com.example.javajigi.mvc.JspView;
import com.example.javajigi.mvc.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HomeController implements Controller {

    @Override
    public View execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        QuestionDao questionDao = new QuestionDao();
        request.setAttribute("questions", questionDao.findAll());
        return new JspView("home.jsp");
    }

}