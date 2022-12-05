package com.example.javajigi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CalculatorTest {

    // 테스트 메소드별 중복 코드 분리
    private final Calculator calculator = new Calculator();

    @Test
    public void add() {
        assertEquals(9, calculator.add(6, 3));
    }

    @Test
    public void test() {
        assertEquals(3, calculator.subtract(6, 3));
    }

    @Test
    public void multiply() {
        assertEquals(27, calculator.multiply(9, 3));
    }

    @Test
    public void divide() {
        assertEquals(3, calculator.divide(9, 3));
    }

}