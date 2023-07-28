package com.example.resourceserver.controller;

import com.example.resourceserver.domain.Workout;
import com.example.resourceserver.service.WorkoutService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("workout")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping("")
    public void add(@RequestBody Workout workout, Authentication authentication) {
        workoutService.saveWorkout(workout);
    }

    @GetMapping("")
    public List<Workout> findAll(Authentication authentication) {
        return workoutService.findWorkouts();
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable Integer id) {
        workoutService.deleteWorkout(id);
    }

}
