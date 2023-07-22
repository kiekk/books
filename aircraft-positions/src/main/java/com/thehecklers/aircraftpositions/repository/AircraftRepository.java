package com.thehecklers.aircraftpositions.repository;

import com.thehecklers.aircraftpositions.domain.Aircraft;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AircraftRepository extends ReactiveCrudRepository<Aircraft, Long> {
}
