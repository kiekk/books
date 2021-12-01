package com.example.recipe2173.calculator;

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

    // 포인트컷 표현식 조합
    // && : and
    // || : or
    // ! : not
    @Pointcut("arithmeticOperation() || unitOperation()")
    public void loggingOperation() {
    }
}
