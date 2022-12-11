package com.example.javajigi.mvc;

import com.example.javajigi.controller.Controller;
import org.apache.logging.log4j.util.Strings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ForwardController implements Controller {

    private String forwardUrl;

    public ForwardController(String forwardUrl) {
        this.forwardUrl = forwardUrl;
        if (Strings.isEmpty(forwardUrl)) {
            throw new NullPointerException("forwardUrl is null. 이동할 URL을 입력하세요");
        }
    }

    @Override
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return forwardUrl;
    }
}
