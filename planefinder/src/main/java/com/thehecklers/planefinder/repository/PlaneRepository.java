package com.thehecklers.planefinder.repository;

import com.thehecklers.planefinder.domain.Aircraft;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PlaneRepository extends ReactiveCrudRepository<Aircraft, Long> {
}
