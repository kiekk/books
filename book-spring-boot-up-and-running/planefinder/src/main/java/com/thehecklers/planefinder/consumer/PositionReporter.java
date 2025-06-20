package com.thehecklers.planefinder.consumer;

import com.thehecklers.planefinder.domain.Aircraft;
import com.thehecklers.planefinder.service.PlaneFinderService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Configuration
@AllArgsConstructor
public class PositionReporter {
    private final PlaneFinderService pfService;

    @Bean
    Supplier<Flux<Aircraft>> reportPositions() {
        return pfService::getAircraft;
    }
}