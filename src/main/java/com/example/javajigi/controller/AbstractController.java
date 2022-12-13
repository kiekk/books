package com.example.javajigi.controller;

import com.example.javajigi.mvc.JsonView;
import com.example.javajigi.mvc.JspView;
import com.example.javajigi.mvc.ModelAndView;

public abstract class AbstractController implements Controller {
    protected ModelAndView jspView(String forwardUrl) {
        return new ModelAndView(new JspView(forwardUrl));
    }

    protected ModelAndView jsonView() {
        return new ModelAndView(new JsonView());
    }
}
