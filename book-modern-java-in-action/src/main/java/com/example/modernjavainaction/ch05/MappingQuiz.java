package com.example.modernjavainaction.ch05;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MappingQuiz {
    public static void main(String[] args) {
        // 1. 숫자 리스트가 주어졌을 때 각 숫자의 제곱근으로 이루어진 리스트를 반환하시오.
        // 예를 들어, [1, 2, 3, 4, 5] 가 주어지면 [1, 4, 9, 16, 25] 를 반환해야 한다.

        List<Integer> numberList = Stream.of(1, 2, 3, 4, 5)
                .map(i -> i * i)
                .collect(Collectors.toList());
        // [1, 4, 9, 16, 25]
        System.out.println(numberList);

        // 2. 두 개의 숫자 리스트가 있을 때 모든 숫자 쌍의 리스트를 반환하시오.
        // 예를 들어 두 개의 리스트 [1, 2, 3] 과 [3, 4] 가 주어지면 [(1, 3), (1, 4), (2, 3), (2, 4), (3, 3), (3, 4)] 를 반환해야 한다.

        List<Integer> number1 = Arrays.asList(1, 2, 3);
        List<Integer> number2 = Arrays.asList(3, 4);
        /*
            [1, 3]
            [1, 4]
            [2, 3]
            [2, 4]
            [3, 3]
            [3, 4]
         */
        number1.stream()
                .flatMap(n1 -> number2.stream().map(n2 -> new int[]{n1, n2}))
                .forEach(r -> System.out.println(Arrays.toString(r)));

        System.out.println("----");

        // 3. 이전 예제에서 합이 3으로 나누어떨어지는 쌍만 반환하려면 어떻게 해야 할까?
        // 예를 들어 (2, 4), (3, 3) 을 반환해야 한다.

        /*
            [2, 4]
            [3, 3]
         */
        number1.stream()
                .flatMap(n1 -> number2
                        .stream()
                        .filter(n2 -> (n1 + n2) % 3 == 0)
                        .map(n2 -> new int[]{n1, n2}))
                .forEach(r -> System.out.println(Arrays.toString(r)));
    }
}
