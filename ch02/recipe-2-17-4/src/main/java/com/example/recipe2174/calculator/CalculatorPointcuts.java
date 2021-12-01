package com.example.recipe2174.calculator;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class CalculatorPointcuts {

    @Pointcut("within(ArithmeticCalculator+)")
    public void arithmeticOperation() {
    }

    @Pointcut("within(UnitCalculator+)")
    public void unitOperation() {
    }

    @Pointcut("arithmeticOperation() || unitOperation()")
    public void loggingOperation() {
    }

    // 포인트컷 매개변수

    @Pointcut("execution(* *.*(..)) && target(target) && args(a,b)")
    public void parameterPointcut(Object target, double a, double b) {
    }
}
