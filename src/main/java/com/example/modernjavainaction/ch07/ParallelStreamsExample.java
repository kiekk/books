package com.example.modernjavainaction.ch07;

public class ParallelStreamsExample {
    public static long iterativeSum(long n) {
        long result = 0;
        for (long i = 0; i <= n; i++) {
            result += i;
        }
        return result;
    }
}
