package io.spring.batch.helloworld;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job() {
        return jobBuilderFactory.get("basicJob")
                .start(systemCommandStep())
                .build();
    }

    @Bean
    public Step systemCommandStep() {
        return stepBuilderFactory.get("systemCommandStep")
                .tasklet(systemCommandTasklet())
                .build();
    }

    @Bean
    public Tasklet systemCommandTasklet() {
        SystemCommandTasklet systemCommandTasklet = new SystemCommandTasklet();
        systemCommandTasklet.setCommand("rm -rf /tmp.txt");
        systemCommandTasklet.setTimeout(5000);
        systemCommandTasklet.setInterruptOnCancel(true);
        return systemCommandTasklet;
    }

}
