package com.example.modernjavainaction.ch03;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ComparatorExample {

    public static void main(String[] args) {
        List<Apple> inventory = new ArrayList<>(Arrays.asList(
                new Apple(80, Color.GREEN),
                new Apple(155, Color.GREEN),
                new Apple(120, Color.RED)
        ));

        System.out.println("before");
        System.out.println(inventory);
        // [Apple{color=GREEN, weight=80}, Apple{color=GREEN, weight=155}, Apple{color=RED, weight=120}]

        inventory.sort(Comparator.comparing(Apple::getWeight).reversed());

        System.out.println("after");
        System.out.println(inventory);
        // [Apple{color=GREEN, weight=155}, Apple{color=RED, weight=120}, Apple{color=GREEN, weight=80}]
    }
}
