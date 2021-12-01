package com.example.recipe2172;

import com.example.recipe2172.calculator.ArithmeticCalculator;
import com.example.recipe2172.calculator.UnitCalculator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

@SpringBootApplication
public class Recipe2172Application {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext("com.example.recipe2172.calculator");

        ArithmeticCalculator arithmeticCalculator =
                (ArithmeticCalculator) context.getBean("arithmeticCalculator");
        arithmeticCalculator.add(1, 2);
        arithmeticCalculator.sub(4, 3);
        arithmeticCalculator.mul(2, 3);
        arithmeticCalculator.div(4, 2);

        UnitCalculator unitCalculator =
                (UnitCalculator) context.getBean("unitCalculator");
        unitCalculator.kilogramToPound(10);
        unitCalculator.kilometerToMile(5);
    }

}
