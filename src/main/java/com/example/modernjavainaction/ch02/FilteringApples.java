package com.example.modernjavainaction.ch02;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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

        // [FilteringApples.Apple(weight=80, color=GREEN), FilteringApples.Apple(weight=155, color=GREEN)]
        List<Apple> greenApples3 = filter(inventory, new AppleColorPredicate());
        System.out.println(greenApples3);

        // [FilteringApples.Apple(weight=155, color=GREEN)]
        List<Apple> heavyApples2 = filter(inventory, new AppleWeightPredicate());
        System.out.println(heavyApples2);

        System.out.println("ApplePrintInfoPredicate");
        /*
            Apple{color=GREEN, weight=80}
            Apple{color=GREEN, weight=155}
            Apple{color=RED, weight=120}
         */
        printApple(inventory, new ApplePrintInfoPredicate());
        System.out.println("AppleIsHeavyPredicate");
        /*
            Apple{color=GREEN, weight=80} is not heavy
            Apple{color=GREEN, weight=155} is heavy
            Apple{color=RED, weight=120} is not heavy
         */
        printApple(inventory, new AppleIsHeavyPredicate());


        // [FilteringApples.Apple(weight=120, color=RED)]
        List<Apple> redApples2 = filter(inventory, apple -> apple.getColor() == Color.RED);
        System.out.println(redApples2);

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

    public static List<Apple> filter(List<Apple> inventory, ApplePredicate p) {
        List<Apple> result = new ArrayList<>();
        for (Apple apple : inventory) {
            if (p.test(apple)) {
                result.add(apple);
            }
        }
        return result;
    }

    public static void printApple(List<Apple> inventory, ApplePrintPredicate p) {
        for (Apple apple : inventory) {
            System.out.println(p.prettyPrintApple(apple));
        }
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

    interface ApplePrintPredicate {
        String prettyPrintApple(Apple a);
    }

    static class ApplePrintInfoPredicate implements ApplePrintPredicate {

        @Override
        public String prettyPrintApple(Apple a) {
            return String.format("Apple{color=%s, weight=%d}", a.getColor(), a.getWeight());
        }
    }

    static class AppleIsHeavyPredicate implements ApplePrintPredicate {

        @Override
        public String prettyPrintApple(Apple a) {
            if (a.getWeight() > 150) {
                return String.format("Apple{color=%s, weight=%d} is heavy", a.getColor(), a.getWeight());
            } else {
                return String.format("Apple{color=%s, weight=%d} is not heavy", a.getColor(), a.getWeight());
            }
        }
    }
}
