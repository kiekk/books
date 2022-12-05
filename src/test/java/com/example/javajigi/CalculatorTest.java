package com.example.javajigi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CalculatorTest {

    // JUnit 라이브러리를 사용하여 테스트 코드 작성
    // main 메소드로 작성할 경우 결과 값을 개발자가 일일이 수동으로 확인해야 함.
    @Test
    public void add() {
        Calculator calculator = new Calculator();
        assertEquals(9, calculator.add(6, 3));
    }

    @Test
    public void test() {
        Calculator calculator = new Calculator();
        assertEquals(3, calculator.subtract(6, 3));
    }

    @Test
    public void multiply() {
        Calculator calculator = new Calculator();
        assertEquals(27, calculator.multiply(9, 3));
    }

    @Test
    public void divide() {
        Calculator calculator = new Calculator();
        assertEquals(3, calculator.divide(9, 3));
    }

}