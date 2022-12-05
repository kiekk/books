package com.example.javajigi;

public class CalculatorTest {
    public static void main(String[] args) {
        Calculator calculator = new Calculator();

        add(calculator);
        subtract(calculator);
        multiply(calculator);
        divide(calculator);
    }

    // 프로덕션 코드별 별도의 테스트 메소드를 작성하여 테스트 코드를 기능별로 분리
    public static void add(Calculator calculator) {
        System.out.println(calculator.add(9, 3));
    }

    public static void subtract(Calculator calculator) {
        System.out.println(calculator.subtract(9, 3));
    }

    public static void multiply(Calculator calculator) {
        System.out.println(calculator.multiply(9, 3));
    }

    public static void divide(Calculator calculator) {
        System.out.println(calculator.divide(9, 3));
    }
}
