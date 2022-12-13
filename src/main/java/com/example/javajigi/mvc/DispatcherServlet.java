package com.example.javajigi.mvc;

import com.example.javajigi.controller.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "dispatcher", urlPatterns = "*.do", loadOnStartup = 1)
public class DispatcherServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);
    private static final String DEFAULT_REDIRECT_PREFIX = "redirect:";

    private RequestMapping rm;

    @Override
    public void init() throws ServletException {
        rm = new RequestMapping();
        rm.initMapping();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String requestUri = req.getRequestURI();
        logger.debug("Method : {}, Request URI : {}", req.getMethod(), requestUri);

        Controller controller = rm.findController(requestUri);

        if (controller == null) {
            return;
        }

        ModelAndView modelAndView;
        try {
            modelAndView = controller.execute(req, resp);
            View view = modelAndView.getView();
            view.render(modelAndView.getModel(), req, resp);
        } catch (Throwable e) {
            logger.error("Exception : {}", e.getMessage());
            throw new ServletException(e.getMessage());
        }
    }

}