package io.spring.batch.helloworld.batch;

import io.spring.batch.helloworld.domain.Customer;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.builder.MultiResourceItemWriterBuilder;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.FormatterLineAggregator;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.oxm.xstream.XStreamMarshaller;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class JobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public JdbcCursorItemReader<Customer> customerJdbcCursorItemReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Customer>()
                .name("customerItemReader")
                .dataSource(dataSource)
                .sql("select * from customer")
                .rowMapper(new BeanPropertyRowMapper<>(Customer.class))
                .build();
    }

    @Bean
    public MultiResourceItemWriter<Customer> multiFlatFileItemWriter() throws Exception {

        return new MultiResourceItemWriterBuilder<Customer>()
                .name("multiFlatFileItemWriter")
                .delegate(delegateCustomerItemWriter(null))
                .itemCountLimitPerResource(25)
                .resource(new FileSystemResource("multi/customer"))
                .build();
    }

    @Bean
    public Step multiXmlGeneratorStep() throws Exception {
        return stepBuilderFactory.get("multiXmlGeneratorStep")
                .<Customer, Customer>chunk(10)
                .reader(customerJdbcCursorItemReader(null))
                .writer(multiFlatFileItemWriter())
                .build();
    }

    @Bean
    public Job xmlGeneratorJob() throws Exception {
        return jobBuilderFactory.get("xmlGeneratorJob")
                .start(multiXmlGeneratorStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<Customer> delegateCustomerItemWriter(CustomerRecordCountFooterCallback footerCallback) throws Exception {
        BeanWrapperFieldExtractor<Customer> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[]{"firstName", "lastName", "address", "city", "state", "zip"});
        fieldExtractor.afterPropertiesSet();

        FormatterLineAggregator<Customer> lineAggregator = new FormatterLineAggregator<>();

        lineAggregator.setFormat("%s %s lives at %s %s in %s, %s.");
        lineAggregator.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<Customer> itemWriter = new FlatFileItemWriter<>();

        itemWriter.setName("delegateCustomerItemWriter");
        itemWriter.setLineAggregator(lineAggregator);
        itemWriter.setAppendAllowed(true);
        itemWriter.setFooterCallback(footerCallback);

        return itemWriter;
    }

}
