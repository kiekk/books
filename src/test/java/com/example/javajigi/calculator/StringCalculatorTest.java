package com.example.javajigi.calculator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class StringCalculatorTest {

    private StringCalculator stringCalculator;

    @BeforeEach
    public void setUp() {
        stringCalculator = new StringCalculator();
    }

    @Test
    public void add_null_or_empty() {
        assertEquals(0, stringCalculator.add(null));
        assertEquals(0, stringCalculator.add(""));
    }

    @Test
    public void add_one_number() {
        assertEquals(3, stringCalculator.add("3"));
    }

    @Test
    public void add_include_comma() {
        assertEquals(6, stringCalculator.add("1,2,3"));
    }

    @Test
    public void add_custom_delimiter() {
        assertEquals(6, stringCalculator.add("//;\n1;2;3"));
    }

    @Test
    public void add_negative_number() {
        assertThrows(RuntimeException.class, () -> {
            stringCalculator.add("-1,2;5");
        });
    }

    @Test
    public void add_invalid_number() {
        assertThrows(NumberFormatException.class, () -> {
            stringCalculator.add("a,3,2");
        });
    }

}