package com.example.javajigi;

public class Calculator {
    int add(int i, int j) {
        return i + j;
    }

    int subtract(int i, int j) {
        return i - j;
    }

    int multiply(int i, int j) {
        return i * j;
    }

    int divide(int i, int j) {
        return i / j;
    }

    // 문제점
    // 프로덕션 코드와 테스트 코드가 한 클래스에 위치
    // 배포 시 프로덕션 코드와 테스트 코드가 함께 배포되는데,
    // 굳이 테스트 코드는 배포될 필요가 없다.
    public static void main(String[] args) {
        Calculator calculator = new Calculator();
        System.out.println(calculator.add(3, 4));
        System.out.println(calculator.subtract(5, 4));
        System.out.println(calculator.multiply(2, 6));
        System.out.println(calculator.divide(8, 4));
    }
}
