package com.example.recipe233;

import com.example.recipe233.sequence.SequenceGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

@SpringBootApplication
public class Recipe233Application {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext("com.example.recipe233.sequence.config");

        SequenceGenerator generator =
                (SequenceGenerator) context.getBean("sequenceGenerator");

        System.out.println(generator.getSequence());
        System.out.println(generator.getSequence());
    }

}
