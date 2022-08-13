package com.example.modernjavainaction.ch06;

import java.util.*;

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

        // Dishes grouped by type and caloric level: {OTHER={DIET=[Dish(name=rice), Dish(name=season fruit)], NORMAL=[Dish(name=french fries), Dish(name=pizza)]}, FISH={DIET=[Dish(name=prawns)], NORMAL=[Dish(name=salmon)]}, MEAT={DIET=[Dish(name=chicken)], FAT=[Dish(name=pork)], NORMAL=[Dish(name=beef)]}}
        System.out.println("Dishes grouped by type and caloric level: " + groupDishedByTypeAndCaloricLevel());
        // Count dishes in groups: {OTHER=4, FISH=2, MEAT=3}
        System.out.println("Count dishes in groups: " + countDishesInGroups());
        // Max caloric dishes by type: {OTHER=Optional[Dish(name=pizza)], FISH=Optional[Dish(name=salmon)], MEAT=Optional[Dish(name=pork)]}
        System.out.println("Max caloric dishes by type: " + maxCaloricDishesByType());
        // Most caloric dishes by type: {OTHER=Optional[Dish(name=pizza)], FISH=Optional[Dish(name=salmon)], MEAT=Optional[Dish(name=pork)]}
        System.out.println("Most caloric dishes by type: " + mostCaloricDishesByType());
        // Sum calories by type: {OTHER=1550, FISH=850, MEAT=1900}
        System.out.println("Sum calories by type: " + sumCaloriesByType());

        // collector 가 Optional 객체로 반환하는 것이 단점
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

    private static Map<Dish.Type, Map<CaloricLevel, List<Dish>>> groupDishedByTypeAndCaloricLevel() {
        return Dish.menu.stream().collect(
                groupingBy(Dish::getType,
                        groupingBy((Dish dish) -> {
                            if (dish.getCalories() <= 400) {
                                return CaloricLevel.DIET;
                            } else if (dish.getCalories() <= 700) {
                                return CaloricLevel.NORMAL;
                            } else {
                                return CaloricLevel.FAT;
                            }
                        })
                )
        );
    }

    private static Map<Dish.Type, Long> countDishesInGroups() {
        return Dish.menu.stream().collect(groupingBy(Dish::getType, counting()));
    }

    private static Map<Dish.Type, Optional<Dish>> maxCaloricDishesByType() {
        return Dish.menu.stream().collect(
                groupingBy(Dish::getType, maxBy(Comparator.comparingInt(Dish::getCalories))));
    }

    private static Map<Dish.Type, Optional<Dish>> mostCaloricDishesByType() {
        return Dish.menu.stream().collect(
                groupingBy(Dish::getType,
                        reducing((Dish d1, Dish d2) -> d1.getCalories() > d2.getCalories() ? d1 : d2)));
    }

    private static Map<Dish.Type, Integer> sumCaloriesByType() {
        return Dish.menu.stream().collect(groupingBy(Dish::getType,
                summingInt(Dish::getCalories)));
    }

    enum CaloricLevel {DIET, NORMAL, FAT}

}
