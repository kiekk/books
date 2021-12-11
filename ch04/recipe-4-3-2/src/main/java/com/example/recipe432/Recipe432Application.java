package com.example.recipe432;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Recipe432Application {

    public static void main(String[] args) {
        final String uri = "http://localhost:8080/member/{memberId}";
        Map<String, String> params = new HashMap<>();
        params.put("memberId", "2");
        RestTemplate restTemplate = new RestTemplate();
        String result = restTemplate.getForObject(uri, String.class, params);
        System.out.println(result);
    }
}
