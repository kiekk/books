package com.thehecklers.aircraftpositions.controller;

import com.thehecklers.aircraftpositions.domain.Aircraft;
import com.thehecklers.aircraftpositions.repository.AircraftRepository;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Controller
public class PositionController {

    private final AircraftRepository aircraftRepository;
    private final RSocketRequester requester;
    private final WebClient client =
            WebClient.create("http://localhost:7634/aircraft");

    public PositionController(AircraftRepository aircraftRepository,
                              RSocketRequester.Builder builder) {
        this.aircraftRepository = aircraftRepository;
        this.requester = builder.tcp("localhost", 7635);
    }


    @GetMapping("aircraft")
    public String getCurrentAircraftPositions(Model model) {
        Flux<Aircraft> aircraftFlux = aircraftRepository.deleteAll()
                .thenMany(client.get()
                        .retrieve()
                        .bodyToFlux(Aircraft.class)
                        .filter(plane -> !plane.getReg().isEmpty())
                        .flatMap(aircraftRepository::save));

        model.addAttribute("currentPositions", aircraftFlux);
        return "positions";
    }

    @ResponseBody
    @GetMapping(value = "/acstream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Aircraft> getCurrentACPositionsStream() {
        return requester.route("acstream")
                .data("Requesting aircraft positions")
                .retrieveFlux(Aircraft.class);
    }

}
