package com.example.modernjavainaction.ch07;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Stream;

public class ParallelStreamsHarness {

    public static final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool();

    public static void main(String[] args) {
        System.out.println("Iterative Sum done in: " + measurePerf(ParallelStreamsExample::iterativeSum, 10_000_000L) + " msecs");
        System.out.println("Sequential Sum done in: " + measurePerf(ParallelStreamsExample::sequentialSum, 10_000_000L) + " msecs");
        System.out.println("Parallel forkJoinSum done in: " + measurePerf(ParallelStreamsExample::parallelSum, 10_000_000L) + " msecs");
        System.out.println("Range forkJoinSum done in: " + measurePerf(ParallelStreamsExample::rangedSum, 10_000_000L) + " msecs");
        System.out.println("Parallel range forkJoinSum done in: " + measurePerf(ParallelStreamsExample::parallelRangedSum, 10_000_000L) + " msecs");
        System.out.println("SideEffect sum done in: " + measurePerf(ParallelStreamsExample::sideEffectSum, 10_000_000L) + " msecs");
        System.out.println("SideEffect parallel sum done in: " + measurePerf(ParallelStreamsExample::sideEffectParallelSum, 10_000_000L) + " msecs");
        // 병렬 스트림의 단점 : 다수의 스레드에서 동시에 데이터에 접근하는 경우 데이터 레이스 문제가 발생합니다.

        System.out.println("ForkJoin sum done in: " + measurePerf(ForkJoinSumCalculator::forkJoinSum, 10_000_000L) + " msecs");
    }

    public static <T, R> long measurePerf(Function<T, R> f, T input) {
        long fastest = Long.MAX_VALUE;
        for (int i = 0; i < 10; i++) {
            long start = System.nanoTime();
            R result = f.apply(input);
            long duration = (System.nanoTime() - start) / 1_000_000;
            System.out.println("Result: " + result);
            if (duration < fastest) {
                fastest = duration;
            }
        }
        return fastest;
    }

}
