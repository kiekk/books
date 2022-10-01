package com.example.ssia.proxy;

import com.example.ssia.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class AuthenticationServerProxy {

    private final RestTemplate restTemplate;

    @Value("${auth.server.base.url}")
    public String baseUrl;

    public void sendAuth(String username, String password) {
        String url = baseUrl + "/user/auth";

        User user = new User();
        user.setUsername(username);
        user.setPassword(password);

        HttpEntity<User> request = new HttpEntity<>(user);
        restTemplate.postForEntity(url, request, Void.class);
    }

    public boolean sendOTP(String username, String code) {
        String url = baseUrl + "/otp/check";

        User user = new User();
        user.setUsername(username);
        user.setCode(code);

        HttpEntity<User> request = new HttpEntity<>(user);
        ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);

        return response.getStatusCode().equals(HttpStatus.OK);
    }

}
