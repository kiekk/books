package com.example.hacking;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;

@RestController
public class SpringAmqpItemController {

    private final AmqpTemplate template;

    public SpringAmqpItemController(AmqpTemplate template) {
        this.template = template;
    }

    @PostMapping("/items")
    Mono<ResponseEntity<?>> addNewItemUsingSpringAmqp(@RequestBody Mono<Item> item) {
        return item
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(content -> Mono
                        .fromCallable(() -> {
                            template.convertAndSend(
                                    "hacking-spring-boot", "new-items-spring-amqp", content);
                            return ResponseEntity.created(URI.create("/items")).build();
                        }));
    }

}