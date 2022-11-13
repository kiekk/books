package com.example.studyspringwebflow.interceptor;

import com.example.studyspringwebflow.service.BookstoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RequiredArgsConstructor
public class CommonDataHandlerInterceptor implements HandlerInterceptor {

    private final BookstoreService bookstoreService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute("randomBooks", this.bookstoreService.findRandomBooks());
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}
