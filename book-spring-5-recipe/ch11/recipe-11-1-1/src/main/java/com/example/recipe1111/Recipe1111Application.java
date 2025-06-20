package com.example.recipe1111;

import com.example.recipe1111.springbatch.config.BatchConfiguration;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class Recipe1111Application {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(BatchConfiguration.class);

        JobRegistry jobRegistry = context.getBean(JobRegistry.class);
        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        JobRepository jobRepository = context.getBean(JobRepository.class);

        System.out.println("JobRegistry: " + jobRegistry);
        System.out.println("JobLauncher: " + jobLauncher);
        System.out.println("JobRepository: " + jobRepository);
    }

}
