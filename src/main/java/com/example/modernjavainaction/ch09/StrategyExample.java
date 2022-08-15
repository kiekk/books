package com.example.modernjavainaction.ch09;

public class StrategyExample {

    public static void main(String[] args) {
        Validator v1 = new Validator(new IsNumeric());
        System.out.println(v1.validate("aaaa"));
        Validator v2 = new Validator(new IsAllLowerCase());
        System.out.println(v2.validate("bbbb"));
    }

    interface ValidationStrategy {
        boolean execute(String s);
    }

    static private class IsAllLowerCase implements ValidationStrategy {

        @Override
        public boolean execute(String s) {
            return s.matches("[a-z]+");
        }

    }

    static private class IsNumeric implements ValidationStrategy {

        @Override
        public boolean execute(String s) {
            return s.matches("\\d+");
        }

    }

    static private class Validator {

        private final ValidationStrategy strategy;

        public Validator(ValidationStrategy v) {
            strategy = v;
        }

        public boolean validate(String s) {
            return strategy.execute(s);
        }

    }
}
