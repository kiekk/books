package com.example.recipe1174;

import com.example.recipe1174.springbatch.config.BatchConfiguration;
import com.example.recipe1174.springbatch.config.UserJob;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class Recipe1174Application {

    public static void main(String[] args) {
        new AnnotationConfigApplicationContext(BatchConfiguration.class, UserJob.class);
    }

}
