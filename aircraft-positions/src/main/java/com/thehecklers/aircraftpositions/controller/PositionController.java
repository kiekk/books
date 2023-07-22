package com.thehecklers.aircraftpositions.controller;

import com.thehecklers.aircraftpositions.repository.AircraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PositionController {

    private final AircraftRepository aircraftRepository;


    @GetMapping("aircraft")
    public String getCurrentAircraftPositions(Model model) {
        model.addAttribute("currentPositions", aircraftRepository.findAll());
        return "positions";
    }

}
