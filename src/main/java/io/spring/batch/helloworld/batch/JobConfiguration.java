package io.spring.batch.helloworld.batch;

import io.spring.batch.helloworld.domain.transaction.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import javax.sql.DataSource;
import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public AsyncItemProcessor<Transaction, Transaction> asyncItemProcessor() {
        AsyncItemProcessor<Transaction, Transaction> processor = new AsyncItemProcessor<>();

        processor.setDelegate(processor());
        processor.setTaskExecutor(new SimpleAsyncTaskExecutor());

        return processor;
    }

    @Bean
    public ItemProcessor<Transaction, Transaction> processor() {
        return item -> {
            Thread.sleep(5);
            return item;
        };
    }

    @Bean
    public JdbcBatchItemWriter<Transaction> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaction>()
                .dataSource(dataSource)
                .beanMapped()
                .sql("INSERT INTO TRANSACTION (ACCOUNT, AMOUNT, TIMESTAMP) VALUES (:account, :amount, :timestamp)")
                .build();
    }

    @Bean
    public AsyncItemWriter<Transaction> asyncItemWriter() {
        AsyncItemWriter<Transaction> writer = new AsyncItemWriter<>();

        writer.setDelegate(writer(null));

        return writer;
    }

    @Bean
    @StepScope
    public FlatFileItemReader<Transaction> fileTransactionReader(
            @Value("#{jobParameters['inputFlatFile']}") Resource resource) {
        return new FlatFileItemReaderBuilder<Transaction>()
                .name("transactionItemReader")
                .resource(resource)
                .saveState(false)
                .delimited()
                .names("transactionId", "accountId", "description", "credit", "debit", "timestamp")
                .fieldSetMapper(fieldSet -> {
                    Transaction transaction = new Transaction();
                    transaction.setTransactionId(fieldSet.readLong("transactionId"));
                    transaction.setAccountId(fieldSet.readLong("accountId"));
                    transaction.setDescription(fieldSet.readString("description"));
                    transaction.setCredit(fieldSet.readBigDecimal("credit"));
                    transaction.setDebit(fieldSet.readBigDecimal("debit"));
                    transaction.setTimestamp(fieldSet.readDate("timestamp", "yyyy-MM-dd HH:mm:ss"));

                    return transaction;
                })
                .build();
    }

    @Bean
    public Step step1async() {
        return stepBuilderFactory.get("step1async")
                .<Transaction, Future<Transaction>>chunk(100)
                .reader(fileTransactionReader(null))
                .processor(asyncItemProcessor())
                .writer(asyncItemWriter())
                .build();
    }

    @Bean
    public Job asyncJob() {
        return jobBuilderFactory.get("asyncJob")
                .start(step1async())
                .build();
    }

}
