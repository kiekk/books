package com.example.javajigi.controller.qna;

import com.example.javajigi.controller.AbstractController;
import com.example.javajigi.dao.AnswerDao;
import com.example.javajigi.model.Answer;
import com.example.javajigi.mvc.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AddAnswerController extends AbstractController {

    private static final Logger log = LoggerFactory.getLogger(AddAnswerController.class);

    @Override
    public ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Answer answer = new Answer(
                request.getParameter("writer"),
                request.getParameter("contents"),
                Long.parseLong(request.getParameter("questionId")));

        log.debug("answer : {}", answer);

        AnswerDao answerDao = new AnswerDao();
        Answer savedAnswer = answerDao.insert(answer);
        return jsonView().addObject("answer", savedAnswer);
    }
}
