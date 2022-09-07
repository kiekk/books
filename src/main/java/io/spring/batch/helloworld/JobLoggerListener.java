package io.spring.batch.helloworld;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class JobLoggerListener implements JobExecutionListener {

    private static String START_MESSAGE = "%s is beginning execution";
    private static String END_MESSAGE = "%s has completed with the status %s";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        System.out.printf(START_MESSAGE + "%n", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        System.out.printf(
                END_MESSAGE + "%n",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus());
    }
}
