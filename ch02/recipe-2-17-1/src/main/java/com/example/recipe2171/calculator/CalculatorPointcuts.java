package com.example.recipe2171.calculator;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;


@Aspect
public class CalculatorPointcuts {

    @Pointcut("annotation(com.example.recipe2171.calculator.LoggingRequired)")
    public void loggingOperation() {
    }

}
