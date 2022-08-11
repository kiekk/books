package com.example.modernjavainaction.ch06;

import static java.util.stream.Collectors.averagingInt;
import static java.util.stream.Collectors.summingInt;

public class SummarizingExample {

    public static void main(String[] args) {
        // Total calories in menu: 4300
        System.out.println("Total calories in menu: " + calculateTotalCalories());
        // Average calories in menu: 477.77777777777777
        System.out.println("Average calories in menu: " + calculateAverageCalories());
    }

    private static int calculateTotalCalories() {
        return Dish.menu.stream().collect(summingInt(Dish::getCalories));
    }

    // summingInt 는 mapToInt 와 sum 으로 대체 가능
//    private static int calculateTotalCalories() { return Dish.menu.stream().mapToInt(Dish::getCalories).sum(); }

    private static Double calculateAverageCalories() {
        return Dish.menu.stream().collect(averagingInt(Dish::getCalories));
    }
}
