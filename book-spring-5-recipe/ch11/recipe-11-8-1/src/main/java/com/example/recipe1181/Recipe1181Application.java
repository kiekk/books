package com.example.recipe1181;

import com.example.recipe1181.springbatch.config.BatchConfiguration;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Date;

@SpringBootApplication
public class Recipe1181Application {

    public static void main(String[] args) throws Throwable {
        ApplicationContext context = new AnnotationConfigApplicationContext(BatchConfiguration.class);

        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        Job job = context.getBean("dailySalesFigures", Job.class);

        jobLauncher.run(job, new JobParametersBuilder()
                .addDate( "date", new Date() ).toJobParameters());
    }

}
