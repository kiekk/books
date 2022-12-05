package com.example.javajigi;

public class CalculatorTest {
    // 테스트용 클래스를 별도로 작성하여 프로덕션 코드와 테스트 코드를 분리
    public static void main(String[] args) {
        Calculator calculator = new Calculator();
        System.out.println(calculator.add(3, 4));
        System.out.println(calculator.subtract(5, 4));
        System.out.println(calculator.multiply(2, 6));
        System.out.println(calculator.divide(8, 4));
    }
}
