package com.example.javajigi.controller.qna;

import com.example.javajigi.controller.Controller;
import com.example.javajigi.dao.AnswerDao;
import com.example.javajigi.dao.QuestionDao;
import com.example.javajigi.mvc.JspView;
import com.example.javajigi.mvc.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ShowController implements Controller {
    @Override
    public View execute(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long questionId = Long.parseLong(req.getParameter("questionId"));
        QuestionDao questionDao = new QuestionDao();
        AnswerDao answerDao = new AnswerDao();
        req.setAttribute("question", questionDao.findById(questionId));
        req.setAttribute("answers", answerDao.findAllByQuestionId(questionId));
        return new JspView("/qna/show.jsp");
    }
}