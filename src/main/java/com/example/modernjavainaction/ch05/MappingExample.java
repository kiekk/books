package com.example.modernjavainaction.ch05;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MappingExample {
    public static void main(String[] args) {
        // map
        List<String> dishNames = Dish.menu.stream()
                .map(Dish::getName)
                .collect(toList());
        System.out.println(dishNames);

        List<String> words = Arrays.asList("Hello", "World");
        // [5, 5]
        List<Integer> wordLengths = words.stream()
                .map(String::length)
                .collect(toList());
        System.out.println(wordLengths);

        // map
        /*
            [H, e, l, l, o]
            [W, o, r, l, d]
         */
        words.stream()
                .map(word -> word.split(""))
                .distinct()
                .forEach(x -> System.out.println(Arrays.toString(x)));

        // distinct 를 사용한 의도는 중복되는 알파벳이 제거되어 출력되는 것인데, map 을 사용하면 의도한 결과가 출력되지 않음
        // 이는 map 에서 split 을 하게 될 경우 반환 타입이 String<String[]> 이기 때문이다.
    }
}
