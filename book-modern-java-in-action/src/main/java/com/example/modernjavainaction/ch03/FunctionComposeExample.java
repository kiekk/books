package com.example.modernjavainaction.ch03;

import java.util.function.Function;

public class FunctionComposeExample {

    public static void main(String[] args) {
        Function<Integer, Integer> f = x -> x + 1;
        Function<Integer, Integer> g = x -> x * 2;
        Function<Integer, Integer> h = f.andThen(g); // g(f(x)) 또는 (g o f)(x) 로 표현
        Function<Integer, Integer> i = f.compose(g); // f(g(x) 또는 (f o g)(x) 로 표현
        // compose 는 앞 뒤 수식의 위치를 바꿔줍니다.

        // 4
        int result = h.apply(1);
        System.out.println(result);

        // 3
        int resul2 = i.apply(1);
        System.out.println(resul2);
    }

}
