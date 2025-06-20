package com.example.modernjavainaction.ch11;


import java.util.Optional;
import java.util.Properties;


public class ReadPositiveIntParamExample {

    public static void main(String[] args) {
        // Optional.empty
        System.out.println(stringToInt(null));
        // Optional[1]
        System.out.println(stringToInt("1"));

        Properties props = new Properties();
        props.setProperty("a", "5");
        props.setProperty("b", "true");
        props.setProperty("c", "-3");

        // 5
        System.out.println(readDurationImperative(props, "a"));
        // 0
        System.out.println(readDurationImperative(props, "b"));
        // 0
        System.out.println(readDurationImperative(props, "c"));
        // 0
        System.out.println(readDurationImperative(props, "d"));

        // 5
        System.out.println(readDurationWithOptional(props, "a"));
        // 0
        System.out.println(readDurationWithOptional(props, "b"));
        // 0
        System.out.println(readDurationWithOptional(props, "c"));
        // 0
        System.out.println(readDurationWithOptional(props, "d"));
    }

    public static Optional<Integer> stringToInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static int readDurationImperative(Properties props, String name) {
        String value = props.getProperty(name);
        if (value != null) {
            try {
                int i = Integer.parseInt(value);
                if (i > 0) {
                    return i;
                }
            } catch (NumberFormatException nfe) {
            }
        }
        return 0;
    }

    public static int readDurationWithOptional(Properties props, String name) {
        return Optional.ofNullable(props.getProperty(name))
                .flatMap(ReadPositiveIntParamExample::stringToInt)
                .filter(i -> i > 0).orElse(0);
    }

}
