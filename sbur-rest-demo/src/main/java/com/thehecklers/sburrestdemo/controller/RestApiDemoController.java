package com.thehecklers.sburrestdemo.controller;

import com.thehecklers.sburrestdemo.entity.Coffee;
import com.thehecklers.sburrestdemo.repository.coffee.CoffeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("coffees")
public class RestApiDemoController {

    private final CoffeeRepository coffeeRepository;

    private final List<Coffee> coffees = new ArrayList<>();

    public RestApiDemoController(CoffeeRepository coffeeRepository) {
        this.coffeeRepository = coffeeRepository;
        this.coffeeRepository.saveAll(List.of(
                new Coffee("Cafe Cereza"),
                new Coffee("Cafe Ganador"),
                new Coffee("Cafe Lareno"),
                new Coffee("Cafe Tres Pontas"))
        );
    }

    @GetMapping("")
    public List<Coffee> getCoffees() {
        return coffeeRepository.findAll();
    }

    @GetMapping("{id}")
    public Coffee getCoffeeById(@PathVariable String id) {
        return coffees.stream().filter(coffee -> Objects.equals(coffee.getId(), id))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("not found : " + id));
    }

    @PostMapping("")
    @ResponseStatus
    public Coffee postCoffee(@RequestBody Coffee coffee) {
        return coffeeRepository.save(coffee);
    }

    @PutMapping("{id}")
    public ResponseEntity<Coffee> putCoffee(@PathVariable String id,
                                            @RequestBody Coffee coffee) {
        Optional<Coffee> optionalCoffee = coffees.stream()
                .filter(cf -> Objects.equals(cf.getId(), id))
                .findFirst();
        if (optionalCoffee.isEmpty()) {
            return new ResponseEntity<>(postCoffee(coffee), HttpStatus.CREATED);
        }
        Coffee findCoffee = optionalCoffee.get();
        findCoffee.setName(coffee.getName());
        return ResponseEntity.ok(findCoffee);
    }

    @DeleteMapping("{id}")
    public void deleteCoffee(@PathVariable String id) {
        coffees.removeIf(coffee -> Objects.equals(coffee.getId(), id));
    }
}
