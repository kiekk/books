package com.thehecklers.sburredis.planefinder.poller;

import com.thehecklers.sburredis.planefinder.domain.aircraft.Aircraft;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@EnableScheduling
public class PlaneFinderPoller {

    private WebClient client = WebClient.create("http://localhost:7634/aircraft");
    private final RedisConnectionFactory connectionFactory;
    private final RedisOperations<String, Aircraft> redisOperations;

    public PlaneFinderPoller(RedisConnectionFactory connectionFactory, RedisOperations<String, Aircraft> redisOperations) {
        this.connectionFactory = connectionFactory;
        this.redisOperations = redisOperations;
    }

    @Scheduled(fixedRate = 1_000)
    public void pollPlanes() {
        connectionFactory.getConnection().serverCommands().flushDb();

        client.get()
                .retrieve()
                .bodyToFlux(Aircraft.class)
                .filter(plane -> !plane.getReg().isEmpty())
                .toStream()
                .forEach(ac -> redisOperations.opsForValue().set(ac.getReg(), ac));

        redisOperations
                .opsForValue()
                .getOperations()
                .keys("*")
                .forEach(ac -> System.out.println(redisOperations.opsForValue().get(ac)));
    }

}
