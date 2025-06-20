package com.example.recipe211;

import com.example.recipe211.service.SequenceService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class Recipe211Application {

    public static void main(String[] args) {
        ApplicationContext context =
                new AnnotationConfigApplicationContext("com.example.recipe211");

        SequenceService service = context.getBean(SequenceService.class);

        System.out.println(service.getNextValue("IT"));
        System.out.println(service.getNextValue("IT"));
    }

}
