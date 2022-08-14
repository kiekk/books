package com.example.modernjavainaction.ch06;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.partitioningBy;

public class PartitioningPrimeExample {

    public static void main(String[] args) {
        /*
        Numbers partitioned in prime and non-prime:
        {false=
        [4, 6, 8, 9, 10, 12, 14, 15, 16, 18, 20, 21, 22, 24, 25, 26, 27, 28, 30, 32, 33, 34, 35, 36, 38, 39, 40, 42, 44, 45, 46, 48, 49, 50,
         51, 52, 54, 55, 56, 57, 58, 60, 62, 63, 64, 65, 66, 68, 69, 70, 72, 74, 75, 76, 77, 78, 80, 81, 82, 84, 85, 86, 87, 88, 90, 91, 92, 93, 94, 95, 96, 98, 99, 100],
         true=[2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97]}
         */
        System.out.println("Numbers partitioned in prime and non-prime: " + partitionPrimes(100));
    }

    public static Map<Boolean, List<Integer>> partitionPrimes(int n) {
        return IntStream.rangeClosed(2, n).boxed()
                .collect(partitioningBy(PartitioningPrimeExample::isPrime));
    }

    public static boolean isPrime(int candidate) {
        return IntStream.rangeClosed(2, candidate-1)
                .limit((long) Math.floor(Math.sqrt(candidate)) - 1)
                .noneMatch(i -> candidate % i == 0);
    }
}
