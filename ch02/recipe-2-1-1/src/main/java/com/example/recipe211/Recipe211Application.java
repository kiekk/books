package com.example.recipe211;

import com.example.recipe211.sequence.SequenceGenerator;
import com.example.recipe211.sequence.config.SequenceGeneratorConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class Recipe211Application {

    public static void main(String[] args) {
        SpringApplication.run(Recipe211Application.class, args);

        // Bean을 가져오기 위해서는 먼저 스프링 IoC 컨테이너를 인스턴스화 해야 함
        ApplicationContext context =
                new AnnotationConfigApplicationContext(SequenceGeneratorConfiguration.class);

        SequenceGenerator generator = context.getBean(SequenceGenerator.class);

        System.out.println(generator.getSequence());
        System.out.println(generator.getSequence());
    }

}
