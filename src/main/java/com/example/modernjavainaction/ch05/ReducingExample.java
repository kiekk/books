package com.example.modernjavainaction.ch05;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ReducingExample {

    public static void main(String[] args) {

        List<Integer> numbers = Arrays.asList(3, 4, 5, 1, 2);

        // 15
        int sum = numbers.stream().reduce(0, (a, b) -> a + b);
        System.out.println(sum);

        // Integer 의 sum 메소드 사용
        // 15
        int sum2 = numbers.stream().reduce(0, Integer::sum);
        System.out.println(sum2);

        Optional<Integer> optionalSum = numbers.stream().reduce(Integer::sum);
        System.out.println(optionalSum); // Optional[15]
        System.out.println(optionalSum.get()); // 15

        // 초기 값이 없을 경우 합계를 반환할 수 없기 때문에 합계가 없음을 나타낼 수 있도록 Optional 객체로 감싼 결과를 반환합니다.
        // 반환 값이 없을 경우 Optional.empty 를 반환

        int max = numbers.stream().reduce(0, Integer::max);
        System.out.println(max); // 5

        int min = numbers.stream().reduce(0, Integer::min);
        System.out.println(min); // 0

        Optional<Integer> optionalMax = numbers.stream().reduce(Integer::max);
        System.out.println(optionalMax); // Optional[5]
        System.out.println(optionalMax.get()); // 5

        Optional<Integer> optionalMin = numbers.stream().reduce(Integer::min);
        System.out.println(optionalMin); // Optional[1]
        System.out.println(optionalMin.get()); // 0
    }
}
