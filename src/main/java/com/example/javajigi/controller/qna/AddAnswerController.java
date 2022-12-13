package com.example.javajigi.controller.qna;

import com.example.javajigi.controller.Controller;
import com.example.javajigi.dao.AnswerDao;
import com.example.javajigi.model.Answer;
import com.example.javajigi.mvc.JsonView;
import com.example.javajigi.mvc.View;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class AddAnswerController implements Controller {

    private static final Logger log = LoggerFactory.getLogger(AddAnswerController.class);

    @Override
    public View execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Answer answer = new Answer(
                request.getParameter("writer"),
                request.getParameter("contents"),
                Long.parseLong(request.getParameter("questionId")));

        log.debug("answer : {}", answer);

        AnswerDao answerDao = new AnswerDao();
        Answer savedAnswer = answerDao.insert(answer);
        request.setAttribute("answer", savedAnswer);
        return new JsonView();
    }
}
