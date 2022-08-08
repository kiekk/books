package com.example.modernjavainaction.ch05;

import java.util.OptionalInt;
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

        // 칼로리 최댓값 구하기
        OptionalInt max1 = Dish.menu.stream().mapToInt(Dish::getCalories).max();
        System.out.println(max1);

        // 값이 없을 경우 default 값 설정
        int max2 = Dish.menu.stream().mapToInt(Dish::getCalories).max().orElse(1);
        System.out.println(max2);

        // IntStream, DoubleStream, LongStream 의 경우 최댓값, 최솟값을 구할 때 0이라는 기본값 때문에 잘못된 결과가 도출될 수 있다.
        // 스트림에 요소가 있는 상황과 실제 최댓값이 0인 상황을 구별하기 위해 Optional 객체를 사용했었는데,
        // 기본형 특화 스트림과 같이 Optional, OptionalDouble, OptionalLong 세 가지 특화 스트림 버전을 제공한다.

        
    }
}
