package com.example.reactive;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HelloController {

    @RequestMapping("hello")
    public Mono<String> hello() {
        return Mono.just("Hello!");
    }
}
