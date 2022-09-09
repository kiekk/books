package io.spring.batch.helloworld;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.util.List;

@RequiredArgsConstructor
public class ExploringTasklet implements Tasklet {

    private final JobExplorer jobExplorer;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String jobName = chunkContext.getStepContext().getJobName();
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, Integer.MAX_VALUE);

        System.out.printf("There are %d job instances for the job %s\n", jobInstances.size(), jobName);
        System.out.println("They have had the following results");
        System.out.println("-----------------------------------");

        for (JobInstance jobInstance : jobInstances) {
            List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);

            System.out.printf("Instance %d had %d executions\n", jobInstance.getInstanceId(), jobExecutions.size());

            for (JobExecution jobExecution : jobExecutions) {
                System.out.printf("\tExtension %d resulted in Exit Status %s\n", jobExecution.getId(), jobExecution.getExitStatus());
            }
        }
        return RepeatStatus.FINISHED;
    }
}
