package com.example.recipe1215.nosql.config;

import com.mongodb.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.example.recipe1215.nosql")
public class MongoConfiguration extends AbstractReactiveMongoConfiguration {

    public static final String DB_NAME = "vehicledb";

    @Bean
    @Override
    public MongoClient reactiveMongoClient() {
        return (MongoClient) MongoClients.create();
    }


    @Override
    protected String getDatabaseName() {
        return DB_NAME;
    }
}
