package io.spring.batch.helloworld;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Properties;

@EnableBatchProcessing
@SpringBootApplication
public class SpringBatchBookApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBatchBookApplication.class, args);
    }

}
