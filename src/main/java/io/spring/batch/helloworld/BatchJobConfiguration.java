package io.spring.batch.helloworld;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PatternMatchingCompositeLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class BatchJobConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job job() {
        return jobBuilderFactory.get("job")
                .start(copyFileStep())
                .build();
    }

    @Bean
    public Step copyFileStep() {
        return stepBuilderFactory.get("copyFileStep")
                .<Customer, Customer>chunk(10)
                .reader(customerFileReader(null))
                .writer(itemWriter())
                .build();
    }

    @Bean
    @StepScope
    public MultiResourceItemReader multiResourceItemReader(
            @Value("#{jobParameters['customerFile']}") Resource[] inputFiles) {
        ;
        return new MultiResourceItemReaderBuilder<>()
                .name("multiResourceReader")
                .resources(inputFiles)
                .delegate(customerFileReader(null))
                .build();
    }

    @Bean
    @StepScope
    public StaxEventItemReader<Customer> customerFileReader(
            @Value("#{jobParameters['customerFile']}") Resource inputFile
    ) {
        return new StaxEventItemReaderBuilder<Customer>()
                .name("customerFileReader")
                .resource(inputFile)
                .addFragmentRootElements("customer")
                .unmarshaller(customerMarshaller())
                .build();
    }

    @Bean
    public Jaxb2Marshaller customerMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(Customer.class, Transaction.class);

        return marshaller;
    }

    @Bean
    @StepScope
    public FlatFileItemReader customerItemReader() {
        return new FlatFileItemReaderBuilder<Customer>()
                .name("customerItemReader")
                .lineMapper(lineTokenizer())
                .build();
    }

    @Bean
    public ItemWriter itemWriter() {
        return items -> items.forEach(System.out::println);
    }

    @Bean
    public PatternMatchingCompositeLineMapper lineTokenizer() {
        Map<String, LineTokenizer> lineTokenizerMap = new HashMap<>();
        lineTokenizerMap.put("CUST*", customerLineTokenizer());
        lineTokenizerMap.put("TRANS*", transactionLineTokenizer());

        Map<String, FieldSetMapper> fieldSetMapperMap = new HashMap<>();

        BeanWrapperFieldSetMapper<Customer> customerFieldSetMapper = new BeanWrapperFieldSetMapper<>();
        customerFieldSetMapper.setTargetType(Customer.class);

        fieldSetMapperMap.put("CUST*", customerFieldSetMapper);
        fieldSetMapperMap.put("TRANS*", new TransactionFieldSetMapper());

        PatternMatchingCompositeLineMapper lineMappers = new PatternMatchingCompositeLineMapper();
        lineMappers.setTokenizers(lineTokenizerMap);
        lineMappers.setFieldSetMappers(fieldSetMapperMap);

        return lineMappers;
    }

    @Bean
    public DelimitedLineTokenizer transactionLineTokenizer() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("prefix", "accountNumber", "transactionDate", "amount");
        return lineTokenizer;
    }

    @Bean
    public DelimitedLineTokenizer customerLineTokenizer() {
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames("firstNames", "middleInitial", "lastName", "address", "city", "state", "zipCode");
        lineTokenizer.setIncludedFields(1, 2, 3, 4, 5, 6, 7);
        return lineTokenizer;
    }

}
