package com.example.recipe2134.calculator;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CalculatorLoggingAspect {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Before("execution(* *.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        log.info("The method {}() begins with {} ", joinPoint.getSignature().getName(), Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(
            pointcut = "execution(* *.*(..))",
            returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("The method {}() ends with {}", joinPoint.getSignature().getName(), result);
    }

    // @AfterThrowing : 조인포인트 실행 중 예외가 발생한 경우에만 실행
    // @AfterReturning 의 returning 과 같이 발생한 예외를 throwing 속성에 담아 전달할 수 있습니다.
    @AfterThrowing(
            pointcut = "execution(* *.*(..))",
            throwing = "e")
    // Throwable은 모든 예외 클래스의 상위 클래스 이므로 조인포인트에서 발생한 모든 예외를 가져옵니다.
//    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
    public void logAfterThrowing(JoinPoint joinPoint, IllegalArgumentException e) { // 특정 예외만 가져옵니다.
        log.error("Illegal argument {} in {}()", Arrays.toString(joinPoint.getArgs()), joinPoint.getSignature().getName());
    }

}

