package com.thehecklers.aircraftpositions.repository;

import com.thehecklers.aircraftpositions.domain.Aircraft;
import org.springframework.data.repository.CrudRepository;

public interface AircraftRepository extends CrudRepository<Aircraft, Long> {
}
