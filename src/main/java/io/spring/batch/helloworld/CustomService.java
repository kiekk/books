package io.spring.batch.helloworld;

public class CustomService {
    public void serviceMethod(String message) {
        System.out.println("Service method was called");
        System.out.println("message : " + message);
    }
}
