package io.spring.batch.helloworld.batch;

import io.spring.batch.helloworld.domain.Customer;
import lombok.RequiredArgsConstructor;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.HibernateItemWriter;
import org.springframework.batch.item.database.builder.HibernateItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.persistence.EntityManagerFactory;

@Configuration
@RequiredArgsConstructor
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
    public HibernateItemWriter<Customer> customerItemWriter(EntityManagerFactory entityManager) {
        return new HibernateItemWriterBuilder<Customer>()
                .sessionFactory(entityManager.unwrap(SessionFactory.class))
                .build();
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
