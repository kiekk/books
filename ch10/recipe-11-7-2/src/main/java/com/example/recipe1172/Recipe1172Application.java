package com.example.recipe1172;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;

@SpringBootApplication
public class Recipe1172Application {

    public static void main(String[] args) throws Throwable {
        ApplicationContext context = new ClassPathXmlApplicationContext("batch.xml", "user-job.xml");

        JobLauncher jobLauncher = context.getBean("jobLauncher", JobLauncher.class);
        Job job = context.getBean("insertIntoDbFromCsvJob", Job.class);
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
