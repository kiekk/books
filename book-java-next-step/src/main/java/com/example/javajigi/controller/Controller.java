package com.example.javajigi.controller;

import com.example.javajigi.mvc.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Controller {
    ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
