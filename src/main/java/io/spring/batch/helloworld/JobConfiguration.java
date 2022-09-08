package io.spring.batch.helloworld;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.tasklet.SimpleSystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.core.step.tasklet.SystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

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

        systemCommandTasklet.setWorkingDirectory("C:\\study\\spring-batch-book");

        systemCommandTasklet.setSystemProcessExitCodeMapper(touchCodeMapper());
        systemCommandTasklet.setTerminationCheckInterval(5000);
        systemCommandTasklet.setTaskExecutor(new SimpleAsyncTaskExecutor());
        systemCommandTasklet.setEnvironmentParams(new String[]{"JAVA_HOME=/java", "BATCH_HOME=/Users/batch"});

        return systemCommandTasklet;
    }

    @Bean
    public SystemProcessExitCodeMapper touchCodeMapper() {
        return new SimpleSystemProcessExitCodeMapper();
    }

}
