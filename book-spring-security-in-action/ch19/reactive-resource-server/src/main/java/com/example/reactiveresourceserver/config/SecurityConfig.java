package com.example.reactiveresourceserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${jwk.endpoint}")
    private String jwkEndpoint;

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(authz ->
                        authz.anyExchange().authenticated()
                )
                .oauth2ResourceServer(oAuth2ResourceServerSpec ->
                        oAuth2ResourceServerSpec.jwt(jwtSpec -> jwtSpec.jwkSetUri(jwkEndpoint))
                )
                .build();
    }
}
