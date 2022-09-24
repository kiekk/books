package io.spring.batch.helloworld.batch;

import io.spring.batch.helloworld.domain.Customer;
import io.spring.batch.helloworld.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.adapter.PropertyExtractingDelegatingItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@RequiredArgsConstructor
@EnableJpaRepositories(basePackages = "io.spring.batch.helloworld")
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    @StepScope
    public FlatFileItemReader<Customer> customerFileReader(
            @Value("#{jobParameters['customerFile']}") Resource inputFile) {
        return new FlatFileItemReaderBuilder<Customer>()
                .name("customerFileReader")
                .resource(inputFile)
                .delimited()
                .names("firstName", "middleInitial", "lastName", "address", "city", "state", "zip")
                .targetType(Customer.class)
                .build();
    }

    @Bean
    public PropertyExtractingDelegatingItemWriter<Customer> customerItemWriter(CustomerService customerService) {
        PropertyExtractingDelegatingItemWriter<Customer> itemWriter = new PropertyExtractingDelegatingItemWriter<>();
        itemWriter.setTargetObject(customerService);
        itemWriter.setTargetMethod("logCustomerAddress");
        itemWriter.setFieldsUsedAsTargetMethodArguments(new String[]{"address", "city", "state", "zip"});
        return itemWriter;
    }

    @Bean
    public Step jdbcJob() {
        return stepBuilderFactory.get("jdbcStep")
                .<Customer, Customer>chunk(10)
                .reader(customerFileReader(null))
                .writer(customerItemWriter(null))
                .build();
    }

    @Bean
    public Job job() {
        return jobBuilderFactory.get("jdbcJob")
                .start(jdbcJob())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
