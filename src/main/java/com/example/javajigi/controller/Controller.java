package com.example.javajigi.controller;

import com.example.javajigi.mvc.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface Controller {
    View execute(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
