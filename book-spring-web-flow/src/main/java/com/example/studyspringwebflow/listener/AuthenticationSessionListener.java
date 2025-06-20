package com.example.studyspringwebflow.listener;

import com.example.studyspringwebflow.service.AccountService;
import com.example.studyspringwebflow.service.AuthenticationException;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener
public class AuthenticationSessionListener implements HttpSessionListener {

	public static final String AUTHENTICATED_ACCOUNT_KEY = "authenticatedAccount";

	@Override
	public void sessionCreated(HttpSessionEvent httpSessionEvent) {
		AccountService accountService = WebApplicationContextUtils.getWebApplicationContext(
				httpSessionEvent.getSession().getServletContext()).getBean(AccountService.class);

		try {
			httpSessionEvent.getSession().setAttribute(AUTHENTICATED_ACCOUNT_KEY, accountService.login("jd", "secret"));
		} catch (AuthenticationException authenticationException) {
			throw new RuntimeException(authenticationException);
		}
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		// Do nothing
	}
}
