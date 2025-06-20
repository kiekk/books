package com.example.recipe1215.nosql;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface VehicleRepository extends ReactiveMongoRepository<Vehicle, String> {

    Mono<Vehicle> findByVehicleNo(String vehicleNo);

}
