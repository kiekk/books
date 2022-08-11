package com.example.modernjavainaction.ch06;

import static java.util.stream.Collectors.summingInt;

public class SummarizingExample {

    public static void main(String[] args) {
        // Total calories in menu: 4300
        System.out.println("Total calories in menu: " + calculateTotalCalories());
    }

    private static int calculateTotalCalories() {
        return Dish.menu.stream().collect(summingInt(Dish::getCalories));
    }

    // summingInt 는 mapToInt 와 sum 으로 대체 가능
//    private static int calculateTotalCalories() { return Dish.menu.stream().mapToInt(Dish::getCalories).sum(); }
}
