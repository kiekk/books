package com.example.recipe1214.nosql;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {

    Vehicle findByVehicleNo(String vehicleNo);

}
