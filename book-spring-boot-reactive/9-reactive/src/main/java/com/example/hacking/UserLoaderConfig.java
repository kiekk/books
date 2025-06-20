package com.example.hacking;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;

import java.util.List;

import static com.example.hacking.SecurityConfig.INVENTORY;
import static com.example.hacking.SecurityConfig.USER;

@Configuration
public class UserLoaderConfig {

    static String role(String auth) {
        return "ROLE_" + auth;
    }

    @Bean
    CommandLineRunner userLoader(MongoOperations mongoOperations) {
        return args -> {
            mongoOperations.save(
                    new User("soono", "password", List.of(role(USER), role(INVENTORY)))
            );
            mongoOperations.save(
                    new User("soono2", "password", List.of(role(INVENTORY)))
            );
        };
    }
}
