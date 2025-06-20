package com.example.recipe2174.calculator;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;


@Aspect
@Component
@EnableAspectJAutoProxy
public class CalculatorLoggingAspect {

    private Log log = LogFactory.getLog(this.getClass());

    @Pointcut("execution(* *.*(..))")
    private void loggingOperation() {
    }

    @Before("CalculatorLoggingAspect.loggingOperation()")
    public void logBefore(JoinPoint joinPoint) {
        log.info("The method " + joinPoint.getSignature().getName()
                + "() begins with " + Arrays.toString(joinPoint.getArgs()));
    }

    @After("CalculatorLoggingAspect.loggingOperation()")
    public void logAfter(JoinPoint joinPoint) {
        log.info("The method " + joinPoint.getSignature().getName()
                + "() ends");
    }

    @AfterReturning(
            pointcut = "CalculatorLoggingAspect.loggingOperation()",
            returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("The method " + joinPoint.getSignature().getName()
                + "() ends with " + result);
    }

    @AfterThrowing(
            pointcut = "CalculatorLoggingAspect.loggingOperation()",
            throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, IllegalArgumentException e) {
        log.error("Illegal argument " + Arrays.toString(joinPoint.getArgs())
                + " in " + joinPoint.getSignature().getName() + "()");
    }

    @Around("CalculatorLoggingAspect.loggingOperation()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("The method " + joinPoint.getSignature().getName()
                + "() begins with " + Arrays.toString(joinPoint.getArgs()));
        try {
            Object result = joinPoint.proceed();
            log.info("The method " + joinPoint.getSignature().getName()
                    + "() ends with " + result);
            return result;
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument "
                    + Arrays.toString(joinPoint.getArgs()) + " in "
                    + joinPoint.getSignature().getName() + "()");
            throw e;
        }
    }

    @Before("execution(* *.*(..)) && target(target) && args(a,b)")
    public void logParameter(Object target, double a, double b) {
        log.info("Target class : " + target.getClass().getName());
        log.info("Arguments : " + a + ", " + b);
    }

    // 매개변수화한 pointcut을 참조하는 모든 어드바이스는 같은 이름의 인수를 선언해
    // 포인트컷 매개변수를 참조할 수 있습니다.
//    @Before("CalculatorPointcuts.parameterPointcut(target, a, b)")
//    public void logParameter(Object target, double a, double b) {
//        log.info("Target class : " + target.getClass().getName());
//        log.info("Arguments : " + a + ", " + b);
//    }

}