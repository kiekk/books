package com.example.javajigi.http;

public enum HttpMethod {
    GET,
    POST;

    public boolean isPost() {
        return this == POST;
    }
}