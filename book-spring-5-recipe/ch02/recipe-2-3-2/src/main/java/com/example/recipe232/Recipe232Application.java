package com.example.recipe232;

import com.example.recipe232.sequence.SequenceGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

@SpringBootApplication
public class Recipe232Application {

    public static void main(String[] args) {
        ApplicationContext context =
                new GenericXmlApplicationContext("appContext.xml");

        SequenceGenerator generator =
                (SequenceGenerator) context.getBean("sequenceGenerator");

        System.out.println(generator.getSequence());
        System.out.println(generator.getSequence());
    }

}
