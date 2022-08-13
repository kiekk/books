package com.example.modernjavainaction.ch06;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.partitioningBy;

public class PartitioningExample {

    public static void main(String[] args) {
        // Dishes partitioned by vegetarian: {false=[Dish(name=pork), Dish(name=beef), Dish(name=chicken), Dish(name=prawns), Dish(name=salmon)], true=[Dish(name=french fries), Dish(name=rice), Dish(name=season fruit), Dish(name=pizza)]}
        System.out.println("Dishes partitioned by vegetarian: " + partitionByVegetarian());
    }

    private static Map<Boolean, List<Dish>> partitionByVegetarian() {
        return Dish.menu.stream().collect(partitioningBy(Dish::isVegetarian));
    }
}
