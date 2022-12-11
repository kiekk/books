package com.example.javajigi.controller;

import com.example.javajigi.db.DataBase;
import com.example.javajigi.http.HttpRequest;
import com.example.javajigi.http.HttpResponse;
import com.example.javajigi.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateUserController implements Controller {

    private static final Logger log = LoggerFactory.getLogger(CreateUserController.class);

    @Override
    public void service(HttpRequest request, HttpResponse response) {
        User user = new User(
                request.getParameter("userId"),
                request.getParameter("password"),
                request.getParameter("name"),
                request.getParameter("email")
        );
        log.debug("User : {}", user);

        DataBase.addUser(user);
        response.sendRedirect("/index./html");
    }

}
