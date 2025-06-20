package com.thehecklers.sburneo.planefinder.repository;

import com.thehecklers.sburneo.planefinder.domain.Aircraft;
import org.springframework.data.repository.CrudRepository;

public interface AircraftRepository extends CrudRepository<Aircraft, Long> {
}
