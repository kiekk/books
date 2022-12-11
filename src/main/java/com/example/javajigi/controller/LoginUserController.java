package com.example.javajigi.controller;

import com.example.javajigi.db.DataBase;
import com.example.javajigi.http.HttpRequest;
import com.example.javajigi.http.HttpResponse;
import com.example.javajigi.model.User;

public class LoginUserController extends AbstractController {

    @Override
    public void doPost(HttpRequest request, HttpResponse response) {
        User user = DataBase.findUserById(request.getParameter("userId"));
        if (user != null && user.login(request.getParameter("password"))) {
            response.addHeader("Set-Cookie", "logined=true");
        } else {
            response.sendRedirect("/user/login_failed.html");
        }
    }

}
