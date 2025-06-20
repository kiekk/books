package com.example.recipe551.reactive.court;

import reactor.core.publisher.Flux;

public interface ReservationService {

    Flux<Reservation> query(String query);
    Flux<Reservation> findAll();
}
