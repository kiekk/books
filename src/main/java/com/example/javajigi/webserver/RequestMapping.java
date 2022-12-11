package com.example.javajigi.webserver;

import com.example.javajigi.controller.Controller;
import com.example.javajigi.controller.CreateUserController;
import com.example.javajigi.controller.ListUserController;
import com.example.javajigi.controller.LoginUserController;

import java.util.HashMap;
import java.util.Map;

public class RequestMapping {
    private static final Map<String, Controller> controllers = new HashMap<>();

    static {
        controllers.put("/user/create", new CreateUserController());
        controllers.put("/user/login", new LoginUserController());
        controllers.put("/user/list", new ListUserController());
    }

    public static Controller getController(String requestUrl) {
        return controllers.get(requestUrl);
    }

}