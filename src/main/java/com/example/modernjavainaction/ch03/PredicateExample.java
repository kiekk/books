package com.example.modernjavainaction.ch03;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class PredicateExample {
    public static void main(String[] args) {
        List<String> listOfString = new ArrayList<>(Arrays.asList("test", "", "test2", "test3"));
        Predicate<String> nonEmptyStringPredicate = s -> !s.isEmpty();

        // [test, test2, test3]
        List<String> nonEmpty = filter(listOfString, nonEmptyStringPredicate);
        System.out.println(nonEmpty);

        Predicate<Apple> redApple = apple -> apple.getColor() == Color.RED;
        Predicate<Apple> notRedApple = redApple.negate(); // 기존 Predicate 의 결과를 반전

        List<Apple> inventory = new ArrayList<>(Arrays.asList(
                new Apple(80, Color.GREEN),
                new Apple(120, Color.GREEN),
                new Apple(120, Color.RED)
        ));

        List<Apple> redApples = filter(inventory, redApple);
        System.out.println(redApples);
        // [Apple{color=RED, weight=120}]

        List<Apple> notRedApples = filter(inventory, notRedApple);
        System.out.println(notRedApples);
        // [Apple{color=GREEN, weight=80}, Apple{color=GREEN, weight=120}]

    }

    public static <T> List<T> filter(List<T> list, Predicate<T> p) {
        List<T> results = new ArrayList<>();
        for (T t : list) {
            if (p.test(t)) {
                results.add(t);
            }
        }
        return results;
    }

}
