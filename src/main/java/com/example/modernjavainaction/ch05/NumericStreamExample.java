package com.example.modernjavainaction.ch05;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NumericStreamExample {

    public static void main(String[] args) {
        // 칼로리 합계 구하기
        Integer result1 = Dish.menu.stream().map(Dish::getCalories).reduce(0, Integer::sum);
        // 4300
        System.out.println(result1);

        int result2 = Dish.menu.stream().mapToInt(Dish::getCalories).sum();
        // 4300
        System.out.println(result2);

        // boxed: 특화 스트림을 일반 스트림으로 변환
        IntStream intStream = Dish.menu.stream().mapToInt(Dish::getCalories);
        Stream<Integer> boxed = intStream.boxed();
    }
}
