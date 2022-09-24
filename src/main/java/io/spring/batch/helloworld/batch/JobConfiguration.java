package io.spring.batch.helloworld.batch;

import io.spring.batch.helloworld.domain.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.mail.SimpleMailMessageItemWriter;
import org.springframework.batch.item.mail.builder.SimpleMailMessageItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    @StepScope
    public FlatFileItemReader<Customer> customerEmailFileReader(
            @Value("#{jobParameters['customerFile']}") Resource inputFile) {
        return new FlatFileItemReaderBuilder<Customer>()
                .name("customerFileReader")
                .resource(inputFile)
                .delimited()
                .names("firstName", "middleInitial", "lastName", "address", "city", "state", "zip", "email")
                .targetType(Customer.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Customer> customerBatchWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Customer>()
                .namedParametersJdbcTemplate(new NamedParameterJdbcTemplate(dataSource))
                .sql("INSERT INTO CUSTOMER (first_name, middle_initial, last_name, address, city, state, zip, email) " +
                        "VALUES (:firstName, :middleInitial, :lastName, :address, :city, :state, :zip, :email)")
                .beanMapped()
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Customer> customerCursorItemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Customer>()
                .name("customerItemReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM CUSTOMER")
                .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
                .build();
    }

    @Bean
    public SimpleMailMessageItemWriter emailItemWriter(MailSender mailSender) {
        return new SimpleMailMessageItemWriterBuilder()
                .mailSender(mailSender)
                .build();
    }

    @Bean
    public Step importStep() {
        return stepBuilderFactory.get("importStep")
                .<Customer, Customer>chunk(10)
                .reader(customerEmailFileReader(null))
                .writer(customerBatchWriter(null))
                .build();
    }

    @Bean
    public Step emailStep() {
        return stepBuilderFactory.get("emailStep")
                .<Customer, SimpleMailMessage>chunk(10)
                .reader(customerCursorItemReader(null))
                .processor((ItemProcessor<Customer, ? extends SimpleMailMessage>) customer -> {
                    SimpleMailMessage mailMessage = new SimpleMailMessage();
                    mailMessage.setFrom("prospringbatch@gmail.com");
                    mailMessage.setTo(customer.getEmail());
                    mailMessage.setSubject("Welcome!");
                    mailMessage.setText(String.format("Welcome %s %s,\nYou were imported into the system using Spring Batch!",
                            customer.getFirstName(), customer.getLastName()));
                    return mailMessage;
                })
                .writer(emailItemWriter(null))
                .build();
    }

    @Bean
    public Job emailJob() {
        return jobBuilderFactory.get("emailJob")
                .start(importStep())
                .next(emailStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }

}
