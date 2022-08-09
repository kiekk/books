package com.example.modernjavainaction.ch05;

import java.util.stream.Stream;

public class BuildingStreamExample {

    public static void main(String[] args) {
        Stream<String> stream = Stream.of("Java 8", "Lambdas", "In", "Action");
        stream.map(String::toUpperCase).forEach(System.out::println);

        Stream<String> emptyStream = Stream.empty();
        emptyStream.forEach(System.out::println);
    }
}
