package com.thehecklers.sburjpa.planefinder.repository.aircraft;

import com.thehecklers.sburjpa.planefinder.domain.aircraft.Aircraft;
import org.springframework.data.repository.CrudRepository;

public interface AircraftRepository extends CrudRepository<Aircraft, Long> {
}
