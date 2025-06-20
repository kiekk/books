package com.thehecklers.aircraftpositions.controller;

import com.thehecklers.aircraftpositions.domain.Aircraft;
import com.thehecklers.aircraftpositions.repository.AircraftRepository;
import com.thehecklers.aircraftpositions.retriever.PositionRetriever;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@WebFluxTest({PositionController.class})
class PositionControllerTest {

    @MockBean
    private AircraftRepository repository;

    @MockBean
    private PositionRetriever retriever;
    private Aircraft ac1, ac2;

    @BeforeEach
    void setUp() {
        // 스프링 항공기 001 STL(Saint Louis)에서 SFO(San Francisco)로 이동 중
        // 현재 Kansas City 상공 30_000 피트
        ac1 = new Aircraft(1L, "SAL001", "sqwk", "N12345", "SAL001",
                "STL-SFO", "LJ", "ct",
                30000, 280, 440, 0, 0,
                39.2979849, -94.71921, 0D, 0D, 0D,
                true, false,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());

        // 스프링 항공기 002 SFO(San Francisco)에서 STL(Saint Louis)로 이동 중
        // 현재 Kansas City 상공 40_000 피트
        ac2 = new Aircraft(2L, "SAL002", "sqwk", "N54321", "SAL002",
                "SFO-STL", "LJ", "ct",
                40000, 65, 440, 0, 0,
                39.8560963, -104.6759263, 0D, 0D, 0D,
                true, false,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());

        Mockito.when(retriever.retrieveAircraftPositions())
                .thenReturn(List.of(ac1, ac2));
        Mockito.when(repository.findAll())
                .thenReturn(List.of(ac1, ac2));
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void getCurrentAircraftPositions(@Autowired WebTestClient client) {
        final Iterable<Aircraft> acPositions = client.get()
                .uri("/aircraft")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Aircraft.class)
                .returnResult()
                .getResponseBody();

        assertThat(acPositions).isEqualTo(List.of(ac1, ac2));
        assertThat(repository.findAll()).isEqualTo(List.of(ac1, ac2));
    }

}