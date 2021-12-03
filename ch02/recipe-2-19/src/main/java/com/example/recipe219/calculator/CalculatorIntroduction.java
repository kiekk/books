package com.example.recipe219.calculator;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclareParents;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class CalculatorIntroduction {

    @DeclareParents(
            value = "com.example.recipe219.calculator.ArithmeticCalculatorImpl",
            defaultImpl = MaxCalculatorImpl.class)
    public MaxCalculator maxCalculator;

    @DeclareParents(
            value = "com.example.recipe219.calculator.ArithmeticCalculatorImpl",
            defaultImpl = MinCalculatorImpl.class)
    public MinCalculator minCalculator;

    @DeclareParents(
            value = "com.example.recipe219.calculator.*CalculatorImpl",
            defaultImpl = CounterImpl.class)
    public Counter counter;

    @After("execution(* com.example.recipe219.calculator.*Calculator.*(..))"
            + " && this(counter)")
    public void increaseCount(Counter counter) {
        counter.increase();
    }

}
