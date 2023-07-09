package com.thehecklers.sburrestdemo.controller;

import com.thehecklers.sburrestdemo.config.properties.GreetingProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("greeting")
public class GreetingController {

    private final GreetingProperties greetingProperties;

    public GreetingController(GreetingProperties greetingProperties) {
        this.greetingProperties = greetingProperties;
    }

    @GetMapping
    public String getGreeting() {
        return greetingProperties.getName();
    }

    @GetMapping("coffee")
    public String getNameAndCoffee() {
        return greetingProperties.getCoffee();
    }
}
