package com.example.modernjavainaction.ch06;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.modernjavainaction.ch06.Dish.dishTags;
import static java.util.stream.Collectors.*;

public class GroupingExample {

    public static void main(String[] args) {
        // Dishes grouped by type: {OTHER=[Dish(name=french fries), Dish(name=rice), Dish(name=season fruit), Dish(name=pizza)], MEAT=[Dish(name=pork), Dish(name=beef), Dish(name=chicken)], FISH=[Dish(name=prawns), Dish(name=salmon)]}
        System.out.println("Dishes grouped by type: " + groupDishesByType());
        // Dish names grouped by type: {OTHER=[french fries, rice, season fruit, pizza], FISH=[prawns, salmon], MEAT=[pork, beef, chicken]}
        System.out.println("Dish names grouped by type: " + groupDishNamesByType());
        // Dish tags grouped by type: {OTHER=[salty, greasy, natural, light, tasty, fresh, fried], FISH=[roasted, tasty, fresh, delicious], MEAT=[salty, greasy, roasted, fried, crisp]}
        System.out.println("Dish tags grouped by type: " + groupDishTagsByType());
    }

    private static Map<Dish.Type, List<Dish>> groupDishesByType() {
        return Dish.menu.stream().collect(groupingBy(Dish::getType));
    }

    private static Map<Dish.Type, List<String>> groupDishNamesByType() {
        return Dish.menu.stream().collect(
                groupingBy(Dish::getType,
                        mapping(Dish::getName, toList())));
    }

    private static Map<Dish.Type, Set<String>> groupDishTagsByType() {
        return Dish.menu.stream().collect(
                groupingBy(Dish::getType,
                        flatMapping(dish -> dishTags.get(dish.getName()).stream(), toSet())));
    }
}
