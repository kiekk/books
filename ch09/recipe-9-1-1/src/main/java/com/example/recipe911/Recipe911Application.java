package com.example.recipe911;

import com.example.recipe911.vehicle.Vehicle;
import com.example.recipe911.vehicle.VehicleDao;
import com.example.recipe911.vehicle.config.VehicleConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class Recipe911Application {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(VehicleConfiguration.class);

        VehicleDao vehicleDao = context.getBean(VehicleDao.class);
        Vehicle vehicle = new Vehicle("TEM0011", "Red", 4, 4);
        vehicleDao.insert(vehicle);

        vehicle = vehicleDao.findByVehicleNo("TEM0011");
        System.out.println(vehicle);
    }

}
