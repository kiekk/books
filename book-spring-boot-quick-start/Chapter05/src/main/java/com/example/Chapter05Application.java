package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class Chapter05Application {

	public static void main(String[] args) {
//		SpringApplication.run(Chapter05Application.class, args);
		SpringApplication application = new SpringApplication(Chapter05Application.class);
		//WebApplicationType을 NONE으로 설정했기 때문에 내장 톰캣을 구동하지 않고 실행됩니다.
		application.setWebApplicationType(WebApplicationType.NONE);
		application.run(args);
	}

}
