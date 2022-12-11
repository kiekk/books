package com.example.javajigi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan
@SpringBootApplication
public class JavajigiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavajigiApplication.class, args);
    }

}
