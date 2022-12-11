package com.example.javajigi.controller;

import com.example.javajigi.db.DataBase;
import com.example.javajigi.util.UserSessionUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(value = "/users")
public class ListUserController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!UserSessionUtils.isLogined(req.getSession())) {
            resp.sendRedirect("/users/loginForm");
            return;
        }

        req.setAttribute("users", DataBase.findAll());

        RequestDispatcher rd = req.getRequestDispatcher("/user/list.jsp");
        rd.forward(req, resp);
    }
}