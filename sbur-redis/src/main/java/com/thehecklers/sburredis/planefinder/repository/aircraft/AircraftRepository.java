package com.thehecklers.sburredis.planefinder.repository.aircraft;

import com.thehecklers.sburredis.planefinder.domain.aircraft.Aircraft;
import org.springframework.data.repository.CrudRepository;

public interface AircraftRepository extends CrudRepository<Aircraft, Long> {
}
