package com.example.recipe1214;

import com.example.recipe1214.nosql.Vehicle;
import com.example.recipe1214.nosql.VehicleRepository;
import com.example.recipe1214.nosql.config.MongoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.List;

@SpringBootApplication
public class Recipe1214Application {

    public static void main(String[] args) {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(MongoConfiguration.class);
        VehicleRepository repository = ctx.getBean(VehicleRepository.class);

        System.out.println("Number of Vehicles: " + repository.count());

        repository.save(new Vehicle("TEM0001", "RED", 4, 4));
        repository.save(new Vehicle("TEM0002", "RED", 4, 4));

        System.out.println("Number of Vehicles: " + repository.count());

        Vehicle v = repository.findByVehicleNo("TEM0001");

        List<Vehicle> vehicleList = repository.findAll();

        System.out.println("Number of Vehicles: " + vehicleList.size());

        for (Vehicle vehicle : vehicleList) {
            repository.delete(vehicle);
        }

        System.out.println("Number of Vehicles: " + repository.count());

        ((AbstractApplicationContext) ctx).close();
    }

}
