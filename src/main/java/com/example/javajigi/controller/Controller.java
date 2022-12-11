package com.example.javajigi.controller;

import com.example.javajigi.http.HttpRequest;
import com.example.javajigi.http.HttpResponse;

public interface Controller {
    void service(HttpRequest request, HttpResponse response);
}
