package com.example.modernjavainaction.ch05;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BuildingStreamExample {

    public static void main(String[] args) {
        Stream<String> stream = Stream.of("Java 8", "Lambdas", "In", "Action");
        stream.map(String::toUpperCase).forEach(System.out::println);

        Stream<String> emptyStream = Stream.empty();
        emptyStream.forEach(System.out::println);

        Stream<String> nullableStream = Stream.ofNullable(null);
        nullableStream.forEach(System.out::println);

        int[] numbers = {2, 3, 5, 7, 11, 13};
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

        Stream.iterate(new int[]{0, 1}, t -> new int[]{t[1], t[0] + t[1]})
                .limit(10)
                .forEach(t -> System.out.printf("(%d, %d)", t[0], t[1]));

        System.out.println();
        System.out.println("---");

        Stream.iterate(new int[]{0, 1}, t -> new int[]{t[1], t[0] + t[1]})
                .limit(10)
                .map(t -> t[0])
                .forEach(System.out::println);

        Stream.generate(Math::random)
                .limit(10)
                .forEach(System.out::println);

        IntStream.generate(() -> 1)
                .limit(5)
                .forEach(System.out::println);
        System.out.println("---");
        IntStream.generate(new IntSupplier() {
            @Override
            public int getAsInt() {
                return 2;
            }
        }).limit(5).forEach(System.out::println);
        System.out.println("---");
        IntStream.generate(() -> 2).limit(5).forEach(System.out::println);
        System.out.println("---");
        IntSupplier fib = new IntSupplier() {

            private int previous = 0;
            private int current = 1;

            @Override
            public int getAsInt() {
                int nextValue = previous + current;
                previous = current;
                current = nextValue;
                return previous;
            }

        };
        IntStream.generate(fib)
                .limit(10)
                .forEach(System.out::println);

        // 람다를 사용하면 기본적으로 불변 상태지만,
        // Supplier 를 사용하면 가변 상태가 된다.
    }
}
