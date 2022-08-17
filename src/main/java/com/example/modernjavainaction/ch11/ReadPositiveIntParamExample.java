package com.example.modernjavainaction.ch11;


import java.util.Optional;


public class ReadPositiveIntParamExample {

    public static void main(String[] args) {
        // Optional.empty
        System.out.println(stringToInt(null));
        // Optional[1]
        System.out.println(stringToInt("1"));
    }

    public static Optional<Integer> stringToInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

}
