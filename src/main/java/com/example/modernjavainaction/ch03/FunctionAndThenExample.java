package com.example.modernjavainaction.ch03;

import java.util.function.Function;

public class FunctionAndThenExample {

    public static void main(String[] args) {
        Function<String, String> addHeader = Letter::addHeader;
        Function<String, String> transformationPipeline = addHeader.andThen(Letter::checkSpelling).andThen(Letter::addFooter);

        String transformText = transformationPipeline.apply("header");
        System.out.println(transformText);
        // From Raoul, Mario and Alan: header Kind regards

        String transformText2 = transformationPipeline.apply("labda");
        System.out.println(transformText2);
        // From Raoul, Mario and Alan: lambda Kind regards

    }
}