package com.example.javajigi.mvc;

import com.example.javajigi.controller.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.HashMap;
import java.util.Map;

public class RequestMapping {
    private static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);
    private final Map<String, Controller> mappings = new HashMap<>();

    void initMapping() {
        mappings.put("/home.do", new HomeController());
        mappings.put("/users/form.do", new ForwardController("/user/form.jsp"));
        mappings.put("/users/loginForm.do", new ForwardController("/user/login.jsp"));
        mappings.put("/users/list.do", new ListUserController());
        mappings.put("/users/login.do", new LoginController());
        mappings.put("/users/profile.do", new ProfileController());
        mappings.put("/users/logout.do", new LogoutController());
        mappings.put("/users/create.do", new CreateUserController());
        mappings.put("/users/updateForm.do", new UpdateUserController());
        mappings.put("/users/update.do", new UpdateUserController());

        logger.info("Initialized Request Mapping!");
    }

    public Controller findController(String url) {
        return mappings.get(url);
    }

    void put(String url, Controller controller) {
        mappings.put(url, controller);
    }
}