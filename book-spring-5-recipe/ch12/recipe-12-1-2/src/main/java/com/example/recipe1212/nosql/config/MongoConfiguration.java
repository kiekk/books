package com.example.recipe1212.nosql.config;

import com.example.recipe1212.nosql.MongoDBVehicleRepository;
import com.example.recipe1212.nosql.VehicleRepository;
import com.mongodb.MongoClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoConfiguration {

    public static final String DB_NAME = "vehicledb";

    @Bean
    public MongoClient mongo() {
        return new MongoClient();
    }

    @Bean
    public VehicleRepository vehicleRepository(MongoClient mongo) {
        return new MongoDBVehicleRepository(mongo, DB_NAME, " vehicles");
    }

}
