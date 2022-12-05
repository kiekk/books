package com.example.javajigi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CalculatorTest {

    // JUnit 어노테이션을 사용하여 테스트별 setUp 
    private Calculator calculator;

    @BeforeEach
    public void setUp() {
        calculator = new Calculator();
    }

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