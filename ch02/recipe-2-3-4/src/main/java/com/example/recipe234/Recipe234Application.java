package com.example.recipe234;

import com.example.recipe234.sequence.SequenceGenerator;
import com.example.recipe234.sequence.config.SequenceConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class Recipe234Application {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(SequenceConfiguration.class);
        context.refresh();

        SequenceGenerator generator =
                (SequenceGenerator) context.getBean("sequenceGenerator");

        System.out.println(generator.getSequence());
        System.out.println(generator.getSequence());
    }

}
