package com.example.javajigi.controller.qna;

import com.example.javajigi.controller.AbstractController;
import com.example.javajigi.dao.QuestionDao;
import com.example.javajigi.model.Question;
import com.example.javajigi.mvc.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CreateQnaController extends AbstractController {

    private final QuestionDao questionDao = new QuestionDao();

    @Override
    public ModelAndView execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Question question = new Question(
                request.getParameter("writer"),
                request.getParameter("title"),
                request.getParameter("contents")
        );

        questionDao.insert(question);
        return jspView("redirect:/home.do");
    }
}
