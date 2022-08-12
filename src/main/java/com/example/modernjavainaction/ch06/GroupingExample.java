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
        // Caloric dishes grouped by type 1: {OTHER=[Dish(name=french fries), Dish(name=pizza)], MEAT=[Dish(name=pork), Dish(name=beef)]}
        System.out.println("Caloric dishes grouped by type 1: " + groupCaloricDishesByType1());
        // Caloric dishes grouped by type 2: {OTHER=[Dish(name=french fries), Dish(name=pizza)], FISH=[], MEAT=[Dish(name=pork), Dish(name=beef)]}
        System.out.println("Caloric dishes grouped by type 2: " + groupCaloricDishesByType2());
        // groupCaloricDishesByType1 과 같이 filter 를 먼저 하게 되면 FISH 타입 자체가 조회되지 않아 Grouping 대상이 되지 않는다.
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

    private static Map<Dish.Type, List<Dish>> groupCaloricDishesByType1() {
        return Dish.menu.stream().filter(dish -> dish.getCalories() > 500).collect(groupingBy(Dish::getType));
    }

    private static Map<Dish.Type, List<Dish>> groupCaloricDishesByType2() {
        return Dish.menu.stream().collect(
                groupingBy(Dish::getType,
                        filtering(dish -> dish.getCalories() > 500, toList())));
    }

}
