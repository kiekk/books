package com.example.recipe911.vehicle.config;

import com.example.recipe911.vehicle.JdbcVehicleDao;
import com.example.recipe911.vehicle.VehicleDao;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class VehicleConfiguration {

    @Bean
    public VehicleDao vehicleDao() {
        return new JdbcVehicleDao(dataSource());
    }

    @Bean
    public DataSource dataSource() {

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setUsername("postgres");
        dataSource.setPassword("password");
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/vehicle");
        dataSource.setMinimumIdle(2);
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }
}
