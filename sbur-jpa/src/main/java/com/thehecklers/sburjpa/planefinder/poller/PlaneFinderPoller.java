package com.thehecklers.sburjpa.planefinder.poller;

import com.thehecklers.sburjpa.planefinder.domain.aircraft.Aircraft;
import com.thehecklers.sburjpa.planefinder.repository.aircraft.AircraftRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@EnableScheduling
public class PlaneFinderPoller {

    private WebClient client = WebClient.create("http://localhost:7634/aircraft");
    private final AircraftRepository aircraftRepository;

    public PlaneFinderPoller(AircraftRepository aircraftRepository) {
        this.aircraftRepository = aircraftRepository;
    }

    @Scheduled(fixedRate = 1_000)
    public void pollPlanes() {
        aircraftRepository.deleteAll();

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
