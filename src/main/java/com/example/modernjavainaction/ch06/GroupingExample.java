package com.example.modernjavainaction.ch06;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;

public class GroupingExample {

    public static void main(String[] args) {
        // Dishes grouped by type: {OTHER=[Dish(name=french fries), Dish(name=rice), Dish(name=season fruit), Dish(name=pizza)], MEAT=[Dish(name=pork), Dish(name=beef), Dish(name=chicken)], FISH=[Dish(name=prawns), Dish(name=salmon)]}
        System.out.println("Dishes grouped by type: " + groupDishesByType());
    }

    private static Map<Dish.Type, List<Dish>> groupDishesByType() {
        return Dish.menu.stream().collect(groupingBy(Dish::getType));
    }
}
