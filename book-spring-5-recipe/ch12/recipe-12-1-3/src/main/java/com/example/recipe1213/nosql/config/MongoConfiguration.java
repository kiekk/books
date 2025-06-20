package com.example.recipe1213.nosql.config;

import com.example.recipe1213.nosql.MongoDBVehicleRepository;
import com.example.recipe1213.nosql.VehicleRepository;
import com.mongodb.client.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class MongoConfiguration {

    public static final String DB_NAME = "vehicledb";

    @Bean
    public MongoTemplate mongo(MongoClient mongo) throws Exception {
        return new MongoTemplate(mongo, DB_NAME);
    }

    @Bean
    public MongoClientFactoryBean mongoFactoryBean() {
        return new MongoClientFactoryBean();
    }


    @Bean
    public VehicleRepository vehicleRepository(MongoTemplate mongo) {
        return new MongoDBVehicleRepository(mongo, "vehicles");
    }
}
