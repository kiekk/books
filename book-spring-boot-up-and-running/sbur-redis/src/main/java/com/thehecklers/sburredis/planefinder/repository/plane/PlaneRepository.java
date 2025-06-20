package com.thehecklers.sburredis.planefinder.repository.plane;

import com.thehecklers.sburredis.planefinder.domain.aircraft.Aircraft;
import org.springframework.data.repository.CrudRepository;

public interface PlaneRepository extends CrudRepository<Aircraft, Long> {
}
