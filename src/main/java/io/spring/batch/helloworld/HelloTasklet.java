package io.spring.batch.helloworld;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

public class HelloTasklet implements Tasklet {

    private static final String HELLO_MESSAGE = "Hello, %s";

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String name = (String) chunkContext.getStepContext().getJobParameters().get("name");

        ExecutionContext jobContext = chunkContext.getStepContext()
                .getStepExecution()
                .getExecutionContext();

        jobContext.put("user.name", name);
        System.out.printf(HELLO_MESSAGE + "%n", name);
        return RepeatStatus.FINISHED;
    }
}
