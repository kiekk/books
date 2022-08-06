package com.example.modernjavainaction.ch05;

import com.example.modernjavainaction.ch04.Dish;

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

    }
}
