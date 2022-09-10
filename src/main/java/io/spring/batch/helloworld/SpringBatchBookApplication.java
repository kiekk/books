package io.spring.batch.helloworld;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchBookApplication {

    public static void main(String[] args) {
        List<String> realArgs = new ArrayList<>(Arrays.asList(args));

        realArgs.add("transactionFile=input/transactionFile.csv");
        realArgs.add("summaryFile=file:///C:/study/spring-batch-book/tmp/summaryFile3.csv");

        SpringApplication.run(SpringBatchBookApplication.class, realArgs.toArray(new String[realArgs.size()]));
    }

}
