package com.example.recipe914;

import com.example.recipe914.vehicle.Vehicle;
import com.example.recipe914.vehicle.VehicleDao;
import com.example.recipe914.vehicle.config.VehicleConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;

@SpringBootApplication
public class Recipe914Application {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(VehicleConfiguration.class);

        VehicleDao vehicleDao = context.getBean(VehicleDao.class);
        Vehicle vehicle1 = new Vehicle("TEM0022", "Blue", 4, 4);
        Vehicle vehicle2 = new Vehicle("TEM0023", "Black", 4, 6);
        Vehicle vehicle3 = new Vehicle("TEM0024", "Green", 4, 5);
        vehicleDao.insert(Arrays.asList(vehicle1, vehicle2, vehicle3));

        vehicleDao.findAll().forEach(System.out::println);
    }

}
