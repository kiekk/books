package com.example.modernjavainaction.ch05;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

public class BuildingStreamExample {

    public static void main(String[] args) {
        Stream<String> stream = Stream.of("Java 8", "Lambdas", "In", "Action");
        stream.map(String::toUpperCase).forEach(System.out::println);

        Stream<String> emptyStream = Stream.empty();
        emptyStream.forEach(System.out::println);

        Stream<String> nullableStream = Stream.ofNullable(null);
        nullableStream.forEach(System.out::println);

        int[] numbers = { 2, 3, 5, 7, 11, 13 };
        System.out.println(Arrays.stream(numbers).sum());

        try {
            long uniqueWords = Files.lines(Paths.get("C:\\study\\modern-java-in-action\\src\\main\\resources\\modernjavainaction\\chap05\\data.txt"), Charset.defaultCharset())
                    .flatMap(line -> Arrays.stream(line.split(" ")))
                    .distinct()
                    .count();

            System.out.println("There are " + uniqueWords + " unique words in data.txt");
        } catch (Exception e) {
            System.out.println("File Exception");
        }
        // Stream 은 AutoCloseable 인터페이스르 구현했기 때문에 자원을 개발자가 직접 닫아줄 필요가 없다.

        Stream.iterate(0, n -> n + 2)
                .limit(10)
                .forEach(System.out::println);
    }
}
