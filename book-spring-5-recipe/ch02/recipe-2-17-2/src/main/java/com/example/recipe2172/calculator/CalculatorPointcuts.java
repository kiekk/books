package com.example.recipe2172.calculator;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;


@Aspect
public class CalculatorPointcuts {

    // 타입 시그니처 패턴
    // within(com.example.recipe2172.calculator.*
    // calculator 패키지의 전체 메소드 실행 조인포인트를 매치합니다.


    // within(com.example.recipe2172.calculator..*
    // 하위 패키지도 함께 매핑

    // witnin(ArithmeticCalculator+)
    // + 기호 : ArithmeticCalculator 인터페이스를 구현한 모든 클래스의 메소드 실행 조인포인트를 매치합니다.
    @Pointcut("within(com.example.recipe2172.calculator.LoggingRequired)")
    public void loggingOperation() {
    }

}
