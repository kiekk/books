package com.example.hacking;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;

import java.util.List;

@Configuration
public class UserLoaderConfig {

    @Bean
    CommandLineRunner userLoader(MongoOperations mongoOperations) {
        return args -> mongoOperations.save(new User("soono", "password", List.of("ROLE_USER")));
    }
}
