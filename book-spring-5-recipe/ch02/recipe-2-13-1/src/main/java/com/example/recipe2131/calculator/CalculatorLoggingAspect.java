package com.example.recipe2131.calculator;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CalculatorLoggingAspect {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    // Aspect 가 적용될 시점 정의
//    @Before("execution(* *.*(..))") // 모든 클래스의 모든 메소드에서 실행
    @Before("execution(* ArithmeticCalculator.add(..))") // ArithmeticCalculator 클래스의 add 메소드에서만 실행
    //                ↑                           ↑ 인수 개수는 몇개가 와도 가능
    //                모든 수정자(public, protected, private) 가능
    public void logBefore(JoinPoint joinPoint) {
        log.info("The method {}() begins with {} ", joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
    }

}