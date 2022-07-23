package com.example.modernjavainaction.ch01;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

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

        // [FilteringApples.Apple(weight=80, color=green), FilteringApples.Apple(weight=155, color=green)]
        List<Apple> greenApples2 = filterApples(inventory, FilteringApples::isGreenApple);
        System.out.println(greenApples2);

        // [FilteringApples.Apple(weight=155, color=green)]
        List<Apple> heavyApples2 = filterApples(inventory, FilteringApples::isHeavyApple);
        System.out.println(heavyApples2);

        // [FilteringApples.Apple(weight=80, color=green), FilteringApples.Apple(weight=155, color=green)]
        List<Apple> greenApples3 = filterApples(inventory, (Apple a) -> "green".equals(a.getColor()));
        System.out.println(greenApples3);

        // [FilteringApples.Apple(weight=155, color=green)]
        List<Apple> heavyApples3 = filterApples(inventory, (Apple a) -> a.getWeight() > 150);
        System.out.println(heavyApples3);

        // []
        List<Apple> weirdApples = filterApples(inventory, (Apple a) -> a.getWeight() < 80 || "brown".equals(a.getColor()));
        System.out.println(weirdApples);
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

    public static boolean isGreenApple(Apple apple) {
        return "green".equals(apple.getColor());
    }

    public static boolean isHeavyApple(Apple apple) {
        return apple.getWeight() > 150;
    }

    public static List<Apple> filterApples(List<Apple> inventory, Predicate<Apple> p) {
        List<Apple> result = new ArrayList<>();
        for (Apple apple : inventory) {
            if (p.test(apple)) {
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
