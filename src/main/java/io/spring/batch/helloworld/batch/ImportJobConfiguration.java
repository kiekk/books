package io.spring.batch.helloworld.batch;

import io.spring.batch.helloworld.domain.customer.CustomerUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.file.transform.PatternMatchingCompositeLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class ImportJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job() throws Exception {
        return jobBuilderFactory.get("importJob")
                .start(importCustomerUpdates())
                .build();
    }

    @Bean
    public Step importCustomerUpdates() throws Exception {
        return stepBuilderFactory.get("importCustomerUpdates")
                .<CustomerUpdate, CustomerUpdate>chunk(100)
                .reader(customerUpdateItemReader(null))
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CustomerUpdate> customerUpdateItemReader(
            @Value("#{jobParameters['customerUpdateFile']}") Resource inputFile) throws Exception {
        return new FlatFileItemReaderBuilder<CustomerUpdate>()
                .name("customerUpdateItemReader")
                .lineTokenizer(customerUpdatesLineTokenizer())
                .resource(inputFile)
                .build();
    }

    @Bean
    public LineTokenizer customerUpdatesLineTokenizer() throws Exception {
        DelimitedLineTokenizer recordType1 = new DelimitedLineTokenizer();
        recordType1.setNames("recordId", "customerId", "firstName", "middleName", "lastName");
        recordType1.afterPropertiesSet();

        DelimitedLineTokenizer recordType2 = new DelimitedLineTokenizer();
        recordType2.setNames("recordId", "customerId", "address1", "address2", "city", "state", "postalCode");
        recordType2.afterPropertiesSet();

        DelimitedLineTokenizer recordType3 = new DelimitedLineTokenizer();
        recordType3.setNames("recordId", "customerId", "emailAddress", "homePhone", "cellPhone", "workPhone", "notificationPreference");
        recordType3.afterPropertiesSet();

        Map<String, LineTokenizer> tokenizers = new HashMap<>();
        tokenizers.put("1*", recordType1);
        tokenizers.put("2*", recordType2);
        tokenizers.put("3*", recordType3);

        PatternMatchingCompositeLineTokenizer lineTokenizer = new PatternMatchingCompositeLineTokenizer();
        lineTokenizer.setTokenizers(tokenizers);

        return lineTokenizer;
    }

}
