package io.spring.batch.helloworld;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Tasklet passTasklet() {
        return (contribution, chunkContext) -> RepeatStatus.FINISHED;
    }

    @Bean
    public Tasklet successTasklet() {
        return (contribution, chunkContext) -> {
            System.out.println("Success!");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Tasklet failTasklet() {
        return (contribution, chunkContext) -> {
            System.out.println("Failure");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Job job() {
        return jobBuilderFactory.get("conditionalJob")
                .start(firstStep())
                .on("FAILED").end()
                .from(decider())
                .on("*").to(successStep())
                .end()
                .build();
    }

    @Bean
    public JobExecutionDecider decider() {
        return new RandomDecider();
    }

    @Bean
    public Step firstStep() {
        return stepBuilderFactory.get("firstStep")
                .tasklet(passTasklet())
                .build();
    }

    @Bean
    public Step successStep() {
        return stepBuilderFactory.get("successStep")
                .tasklet(successTasklet())
                .build();
    }

    @Bean
    public Step failureStep() {
        return stepBuilderFactory.get("failureStep")
                .tasklet(failTasklet())
                .build();
    }

}
