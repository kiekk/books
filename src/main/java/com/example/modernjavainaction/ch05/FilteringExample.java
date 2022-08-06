package com.example.modernjavainaction.ch05;

import com.example.modernjavainaction.ch04.Dish;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class FilteringExample {

    public static void main(String[] args) {
        // Predicate 로 filtering - 기본 사용 방식
        /*
            Dish(name=french fries)
            Dish(name=rice)
            Dish(name=season fruit)
            Dish(name=pizza)
         */
        System.out.println("Filtering with a predicate");
        List<Dish> vegetarianMenu = Dish.menu.stream()
                .filter(Dish::isVegetarian)
                .collect(toList());
        vegetarianMenu.forEach(System.out::println);

        // distinct 로 중복 요소 제거
        System.out.println("Filtering unique elements:");
        List<Integer> numbers = Arrays.asList(1, 2, 1, 3, 3, 2, 4);
        numbers.stream()
                .filter(i -> i % 2 == 0)
                .distinct()
                .forEach(System.out::println);

        List<Dish> specialMenu = Arrays.asList(
                new Dish("rice", true, 350, Dish.Type.OTHER),
                new Dish("chicken", false, 400, Dish.Type.MEAT),
                new Dish("prawns", false, 300, Dish.Type.FISH),
                new Dish("season fruit", true, 120, Dish.Type.OTHER),
                new Dish("french fries", true, 530, Dish.Type.OTHER));
        System.out.println("Filtered sorted menu:");
        /*
            Dish(name=prawns)
            Dish(name=season fruit)
         */
        specialMenu.stream()
                .filter(dish -> dish.getCalories() < 320)
                .collect(toList())
                .forEach(System.out::println);

        System.out.println("-------");

        System.out.println("Sorted menu sliced with takeWhile():");
        /*
            []
         */
        specialMenu.stream()
                .takeWhile(dish -> dish.getCalories() < 320)
                .forEach(System.out::println);

        System.out.println("-------");

        System.out.println("Sorted menu sliced with dropWhile():");
        /*
            Dish(name=rice)
            Dish(name=chicken)
            Dish(name=prawns)
            Dish(name=season fruit)
            Dish(name=french fries)
         */
        specialMenu.stream()
                .dropWhile(dish -> dish.getCalories() < 320)
                .forEach(System.out::println);

        // takeWhile, dropWhile 의 경우 collection 이 정렬 되어 있지 않을 경우 정상적으로 연산이 되지 않음

        System.out.println("-------");

        System.out.println("Sorted menu sliced with sort asc and takeWhile():");
        /*
            Dish(name=season fruit)
            Dish(name=prawns)
         */
        specialMenu.stream()
                .sorted(Comparator.comparing(Dish::getCalories))
                .takeWhile(dish -> dish.getCalories() < 320)
                .forEach(System.out::println);

        System.out.println("-------");

        System.out.println("Sorted menu sliced with sort asc dropWhile():");
        /*
            Dish(name=rice)
            Dish(name=chicken)
            Dish(name=french fries)
         */
        specialMenu.stream()
                .sorted(Comparator.comparing(Dish::getCalories))
                .dropWhile(dish -> dish.getCalories() < 320)
                .forEach(System.out::println);

        System.out.println("-------");

        System.out.println("Sorted menu sliced with sort desc and takeWhile():");
        /*
            []
         */
        specialMenu.stream()
                .sorted(Comparator.comparing(Dish::getCalories).reversed())
                .takeWhile(dish -> dish.getCalories() < 320)
                .forEach(System.out::println);

        System.out.println("-------");

        System.out.println("Sorted menu sliced with sort desc dropWhile():");
        /*
            Dish(name=french fries)
            Dish(name=chicken)
            Dish(name=rice)
            Dish(name=prawns)
            Dish(name=season fruit)
         */
        specialMenu.stream()
                .sorted(Comparator.comparing(Dish::getCalories).reversed())
                .dropWhile(dish -> dish.getCalories() < 320)
                .forEach(System.out::println);

        // 또한 정렬 기준이 오름차순(asc), 내림차순(desc) 에 따라서도 연산 결과가 달라짐
    }
}
