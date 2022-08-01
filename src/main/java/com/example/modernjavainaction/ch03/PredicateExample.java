package com.example.modernjavainaction.ch03;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class PredicateExample {
    public static void main(String[] args) {
        List<String> listOfString = new ArrayList<>(Arrays.asList("test", "", "test2", "test3"));
        Predicate<String> nonEmptyStringPredicate = s -> !s.isEmpty();

        // [test, test2, test3]
        List<String> nonEmpty = filter(listOfString, nonEmptyStringPredicate);
        System.out.println(nonEmpty);
    }

    public static <T> List<T> filter(List<T> list, Predicate<T> p) {
        List<T> results = new ArrayList<>();
        for (T t : list) {
            if (p.test(t)) {
                results.add(t);
            }
        }
        return results;
    }
}
