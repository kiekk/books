package com.example.ssia.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.InMemoryClientDetailsService;

import java.util.List;
import java.util.Map;

@Configuration
@EnableAuthorizationServer
@RequiredArgsConstructor
public class AuthServerConfig extends AuthorizationServerConfigurerAdapter {

    private final AuthenticationManager authenticationManager;

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints.authenticationManager(authenticationManager);
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        InMemoryClientDetailsService inMemoryClientDetailsService = new InMemoryClientDetailsService();
        BaseClientDetails baseClientDetails = new BaseClientDetails();

        baseClientDetails.setClientId("client");
        baseClientDetails.setClientSecret("secret");
        baseClientDetails.setScope(List.of("read"));
        baseClientDetails.setAuthorizedGrantTypes(List.of("password"));

        inMemoryClientDetailsService.setClientDetailsStore(Map.of("client", baseClientDetails));

        clients.withClientDetails(inMemoryClientDetailsService);
    }
}
