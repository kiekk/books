package com.example.modernjavainaction.ch02;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class FilteringApples {

    public static void main(String... args) {
        List<Apple> inventory = Arrays.asList(
                new Apple(80, Color.GREEN),
                new Apple(155, Color.GREEN),
                new Apple(120, Color.RED)
        );

        // [FilteringApples.Apple(weight=80, color=GREEN), FilteringApples.Apple(weight=155, color=GREEN)]
        List<Apple> greenApples = filterApplesByColor(inventory, Color.GREEN);
        System.out.println(greenApples);

        // [Apple{color=RED, weight=120}]
        List<Apple> redApples = filterApplesByColor(inventory, Color.RED);
        System.out.println(redApples);

        // [FilteringApples.Apple(weight=155, color=GREEN)]
        List<Apple> heavyApples150 = filterApplesByWeight(inventory, 150);
        System.out.println(heavyApples150);

        // [FilteringApples.Apple(weight=155, color=GREEN), FilteringApples.Apple(weight=120, color=RED)]
        List<Apple> heavyApples80 = filterApplesByWeight(inventory, 80);
        System.out.println(heavyApples80);

        // [FilteringApples.Apple(weight=80, color=GREEN), FilteringApples.Apple(weight=155, color=GREEN)]
        List<Apple> greenApples2 = filterApples(inventory, Color.GREEN, 0, true);
        System.out.println(greenApples2);

        // [FilteringApples.Apple(weight=155, color=GREEN)]
        List<Apple> heavyApples = filterApples(inventory, null, 150, false);
        System.out.println(heavyApples);

    }

    public static List<Apple> filterApplesByColor(List<Apple> inventory, Color color) {
        List<Apple> result = new ArrayList<>();
        for (Apple apple : inventory) {
            if (apple.getColor() == color) {
                result.add(apple);
            }
        }
        return result;
    }

    public static List<Apple> filterApplesByWeight(List<Apple> inventory, int weight) {
        List<Apple> result = new ArrayList<>();
        for (Apple apple : inventory) {
            if (apple.getWeight() > weight) {
                result.add(apple);
            }
        }
        return result;
    }

    public static List<Apple> filterApples(List<Apple> inventory, Color color, int weight, boolean flag) {
        List<Apple> result = new ArrayList<>();
        for (Apple apple : inventory) {
            if ((flag && apple.getColor() == color) || (!flag && apple.getWeight() > weight)) {
                result.add(apple);
            }
        }
        return result;
    }

    enum Color {
        RED,
        GREEN
    }

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    public static class Apple {

        private int weight;
        private Color color;

    }

    interface ApplePredicate {

        boolean test(Apple a);

    }

    static class AppleWeightPredicate implements ApplePredicate {

        @Override
        public boolean test(Apple apple) {
            return apple.getWeight() > 150;
        }

    }

    static class AppleColorPredicate implements ApplePredicate {

        @Override
        public boolean test(Apple apple) {
            return apple.getColor() == Color.GREEN;
        }

    }

}
