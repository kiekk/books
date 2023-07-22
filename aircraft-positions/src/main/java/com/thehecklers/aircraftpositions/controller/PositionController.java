package com.thehecklers.aircraftpositions.controller;

import com.thehecklers.aircraftpositions.domain.Aircraft;
import com.thehecklers.aircraftpositions.retriever.PositionRetriever;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PositionController {

    private final PositionRetriever retriever;

    @GetMapping("/aircraft")
    public Iterable<Aircraft> getCurrentAircraftPositions() {
        return retriever.retrieveAircraftPositions();
    }
}
