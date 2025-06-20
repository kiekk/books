package com.thehecklers.sburredis.planefinder.poller;

import com.thehecklers.sburredis.planefinder.domain.aircraft.Aircraft;
import com.thehecklers.sburredis.planefinder.repository.aircraft.AircraftRepository;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@EnableScheduling
public class PlaneFinderPoller {

    private WebClient client = WebClient.create("http://localhost:7634/aircraft");
    private final RedisConnectionFactory connectionFactory;
    private final AircraftRepository aircraftRepository;

    public PlaneFinderPoller(RedisConnectionFactory connectionFactory, AircraftRepository aircraftRepository) {
        this.connectionFactory = connectionFactory;
        this.aircraftRepository = aircraftRepository;
    }

    @Scheduled(fixedRate = 1_000)
    public void pollPlanes() {
        connectionFactory.getConnection().serverCommands().flushDb();

        client.get()
                .retrieve()
                .bodyToFlux(Aircraft.class)
                .filter(plane -> !plane.getReg().isEmpty())
                .toStream()
                .forEach(aircraftRepository::save);

        aircraftRepository.findAll()
                .forEach(System.out::println);
    }

}
