package io.spring.batch.helloworld;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job() {
        return jobBuilderFactory.get("job")
                .incrementer(new RunIdIncrementer())
                .start(step1())
                .build();
    }

    @Bean
    public Step step1() {
        return stepBuilderFactory.get("step1")
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("step1 run");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @RestController
    @RequiredArgsConstructor
    public static class JobLaunchingController {

        private final JobLauncher jobLauncher;
        private final ApplicationContext context;
        private final JobExplorer jobExplorer;

        @PostMapping("/run")
        public ExitStatus runJob(@RequestBody JobLaunchRequest request) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
            Job job = context.getBean(request.getName(), Job.class);
            JobParameters jobParameters = new JobParametersBuilder(request.getJobParameters(), jobExplorer)
                    .getNextJobParameters(job)
                    .toJobParameters();
            return jobLauncher.run(job, jobParameters).getExitStatus();
        }

    }

    public static class JobLaunchRequest {
        private String name;
        private Properties jobParameters;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Properties getJobParamsProperties() {
            return jobParameters;
        }

        public void setJobParamsProperties(Properties jobParameters) {
            this.jobParameters = jobParameters;
        }

        public JobParameters getJobParameters() {
            Properties properties = new Properties();
            properties.putAll(jobParameters);
            return new JobParametersBuilder(properties).toJobParameters();
        }
    }

}
