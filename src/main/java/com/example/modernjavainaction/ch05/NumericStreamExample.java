package com.example.modernjavainaction.ch05;

public class NumericStreamExample {

    public static void main(String[] args) {
        // 칼로리 합계 구하기
        Integer result1 = Dish.menu.stream().map(Dish::getCalories).reduce(0, Integer::sum);
        // 4300
        System.out.println(result1);

        int result2 = Dish.menu.stream().mapToInt(Dish::getCalories).sum();
        // 4300
        System.out.println(result2);
    }
}
