package com.example.recipe1162;

import com.example.recipe1162.springbatch.config.BatchConfiguration;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Date;

@SpringBootApplication
public class Recipe1162Application  {

    public static void main(String[] args) throws Throwable {
        ApplicationContext context = new AnnotationConfigApplicationContext(BatchConfiguration.class);

        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        Job job = context.getBean(Job.class);

        JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
        jobParametersBuilder.addDate("date", new Date());
        JobParameters jobParameters = jobParametersBuilder.toJobParameters();

        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        BatchStatus batchStatus = jobExecution.getStatus();

        while (batchStatus.isRunning()) {
            System.out.println("Still running...");
            Thread.sleep(1000);
        }
        System.out.println("Exit status: " + jobExecution.getExitStatus().getExitCode());

        JobInstance jobInstance = jobExecution.getJobInstance();
        System.out.println("job instance Id: " + jobInstance.getId());
    }

}
