package com.thehecklers.aircraftpositions.repository;

import com.thehecklers.aircraftpositions.domain.Aircraft;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AircraftRepositoryTest {

    @Autowired
    private AircraftRepository repository;
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

        repository.saveAll(List.of(ac1, ac2));
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    void testFindById() {
        assertThat(repository.findById(ac1.getId())).isEqualTo(Optional.of(ac1));
        assertThat(repository.findById(ac2.getId())).isEqualTo(Optional.of(ac2));
    }

    @Test
    void testFindAll() {
        assertThat(repository.findAll()).isEqualTo(List.of(ac1, ac2));
    }

}