package com.thehecklers.sburrestdemo.controller;

import com.thehecklers.sburrestdemo.entity.Coffee;
import com.thehecklers.sburrestdemo.repository.coffee.CoffeeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("coffees")
public class RestApiDemoController {

    private final CoffeeRepository coffeeRepository;

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
        return coffeeRepository.findById(id)
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
        return coffeeRepository.existsById(id)
                ? ResponseEntity.ok(coffeeRepository.save(coffee)) :
                new ResponseEntity<>(coffeeRepository.save(coffee), HttpStatus.CREATED);
    }

    @DeleteMapping("{id}")
    public void deleteCoffee(@PathVariable String id) {
        coffeeRepository.deleteById(id);
    }
}
