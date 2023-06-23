package com.thehecklers.sburrestdemo.controller;

import com.thehecklers.sburrestdemo.entity.Coffee;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @GetMapping("{id}")
    public Coffee getCoffeeById(@PathVariable String id) {
        return coffees.stream().filter(coffee -> Objects.equals(coffee.getId(), id))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("not found : " + id));
    }

    @PostMapping("")
    public Coffee postCoffee(@RequestBody Coffee coffee) {
        coffees.add(coffee);
        return coffee;
    }
}
