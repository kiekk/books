package com.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

@Service
public class LoggingRunner implements ApplicationRunner {

	private Logger logger = LoggerFactory.getLogger(LoggingRunner.class);
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		logger.trace("TRACE 로그 메세지");
		logger.debug("DEBUG 로그 메세지");
		logger.info("INFO 로그 메세지");
		logger.warn("WARN 로그 메세지");
		logger.error("ERROR 로그 메세지");
	}
	/*
	 * 스프링 부트의 경우 기본적으로 로그 레벨을 INFO로 처리합니다.
	 * 따라서 INFO 이상의 로그 메세지만 출력되며, 이를 조정하기 위해서는 
	 * application.properties 파일을 수정합니다.
	 */
}
