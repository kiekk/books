package com.example.recipe433;

import com.example.recipe433.domain.Members;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Recipe433Application {

    public static void main(String[] args) {
        final String uri = "http://localhost:8080/members";
        RestTemplate restTemplate = new RestTemplate();
        Members result = restTemplate.getForObject(uri, Members.class);

        result.getMembers().forEach(System.out::println);
    }
}
