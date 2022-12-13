package com.example.javajigi.controller.qna;

import com.example.javajigi.controller.AbstractController;
import com.example.javajigi.dao.AnswerDao;
import com.example.javajigi.dao.QuestionDao;
import com.example.javajigi.mvc.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ShowController extends AbstractController {

    private final QuestionDao questionDao = new QuestionDao();
    private final AnswerDao answerDao = new AnswerDao();

    @Override
    public ModelAndView execute(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long questionId = Long.parseLong(req.getParameter("questionId"));
        return jspView("/qna/show.jsp")
                .addObject("question", questionDao.findById(questionId))
                .addObject("answers", answerDao.findAllByQuestionId(questionId));
    }
}