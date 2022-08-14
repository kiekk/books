package com.example.modernjavainaction.ch06;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.*;

public class PartitioningExample {

    public static void main(String[] args) {
        // Dishes partitioned by vegetarian: {false=[Dish(name=pork), Dish(name=beef), Dish(name=chicken), Dish(name=prawns), Dish(name=salmon)], true=[Dish(name=french fries), Dish(name=rice), Dish(name=season fruit), Dish(name=pizza)]}
        System.out.println("Dishes partitioned by vegetarian: " + partitionByVegetarian());
        // Vegetarian Dishes by type: {false={MEAT=[Dish(name=pork), Dish(name=beef), Dish(name=chicken)], FISH=[Dish(name=prawns), Dish(name=salmon)]}, true={OTHER=[Dish(name=french fries), Dish(name=rice), Dish(name=season fruit), Dish(name=pizza)]}}
        System.out.println("Vegetarian Dishes by type: " + vegetarianDishesByType());
        // Most caloric dishes by vegetarian: {false=Dish(name=pork), true=Dish(name=pizza)}
        System.out.println("Most caloric dishes by vegetarian: " + mostCaloricPartitionedByVegetarian());
    }

    private static Map<Boolean, List<Dish>> partitionByVegetarian() {
        return Dish.menu.stream().collect(partitioningBy(Dish::isVegetarian));
    }

    private static Map<Boolean, Map<Dish.Type, List<Dish>>> vegetarianDishesByType() {
        return Dish.menu.stream().collect(partitioningBy(Dish::isVegetarian, groupingBy(Dish::getType)));
    }

    private static Object mostCaloricPartitionedByVegetarian() {
        return Dish.menu.stream().collect(
                partitioningBy(Dish::isVegetarian,
                        collectingAndThen(
                                maxBy(comparingInt(Dish::getCalories)),
                                Optional::get)));
    }
}
