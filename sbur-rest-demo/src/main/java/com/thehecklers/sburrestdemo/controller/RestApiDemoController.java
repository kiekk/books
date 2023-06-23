package com.thehecklers.sburrestdemo.controller;

import com.thehecklers.sburrestdemo.entity.Coffee;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("coffees")
public class RestApiDemoController {

    private final List<Coffee> coffees = new ArrayList<>();

    public RestApiDemoController() {
        coffees.addAll(List.of(
                new Coffee("Cafe Cereza"),
                new Coffee("Cafe Ganador"),
                new Coffee("Cafe Lareno"),
                new Coffee("Cafe Tres Pontas"))
        );
    }

    @GetMapping("")
    public List<Coffee> getCoffees() {
        return coffees;
    }
}
