package com.example.modernjavainaction.ch01;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilteringApples {

    public static void main(String... args) {
        List<Apple> inventory = Arrays.asList(
                new Apple(80, "green"),
                new Apple(155, "green"),
                new Apple(120, "red")
        );

        // [FilteringApples.Apple(weight=80, color=green), FilteringApples.Apple(weight=155, color=green)]
        List<Apple> greenApples = filterGreenApples(inventory);
        System.out.println(greenApples);

        // [FilteringApples.Apple(weight=155, color=green)]
        List<Apple> heavyApples = filterHeavyApples(inventory);
        System.out.println(heavyApples);
    }

    public static List<Apple> filterGreenApples(List<Apple> inventory) {
        List<Apple> result = new ArrayList<>();
        for (Apple apple : inventory) {
            if ("green".equals(apple.getColor())) {
                result.add(apple);
            }
        }
        return result;
    }

    public static List<Apple> filterHeavyApples(List<Apple> inventory) {
        List<Apple> result = new ArrayList<>();
        for (Apple apple : inventory) {
            if (apple.getWeight() > 150) {
                result.add(apple);
            }
        }
        return result;
    }

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    public static class Apple {

        private int weight;
        private String color;

    }

}
