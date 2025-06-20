package com.example.recipe313.court.service.config;

import com.example.recipe313.court.service.ReservationService;
import com.example.recipe313.court.service.ReservationServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {

    @Bean
    public ReservationService reservationService() {
        return new ReservationServiceImpl();
    }
}
