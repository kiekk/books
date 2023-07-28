package com.example.reactive;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HelloController {

    @RequestMapping("hello")
    public Mono<String> hello(Mono<Authentication> auth) {
        return auth.map(a -> "Hello " + a.getName());
    }

    @RequestMapping("ciao")
    public Mono<String> ciao() {
        return Mono.just("Ciao!");
    }
}
