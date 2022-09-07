package io.spring.batch.helloworld;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class GoodByeTasklet implements Tasklet {

    private static final String GOOD_BYE_MESSAGE = "GoodBye, %s";

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String name = (String) chunkContext.getStepContext().getJobParameters().get("name");

        System.out.printf(GOOD_BYE_MESSAGE + "%n", name);
        return RepeatStatus.FINISHED;
    }
}
