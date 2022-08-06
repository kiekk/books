package com.example.modernjavainaction.ch05;

public class FindingExample {

    public static void main(String[] args) {
        if (isVegetarianFriendlyMenu()) {
            System.out.println("Vegetarian friendly");
        }

        System.out.println(isHealthyMenu());
        System.out.println(isHealthyMenu2());
    }

    private static boolean isVegetarianFriendlyMenu() {
        return Dish.menu.stream().anyMatch(Dish::isVegetarian);
    }

    private static boolean isHealthyMenu() {
        return Dish.menu.stream().allMatch(d -> d.getCalories() < 1000);
    }

    // noneMatch: 주어진 검색 조건과 일치하는 요소가 없을 경우 true
    private static boolean isHealthyMenu2() {
        return Dish.menu.stream().noneMatch(d -> d.getCalories() >= 1000);
    }
}
