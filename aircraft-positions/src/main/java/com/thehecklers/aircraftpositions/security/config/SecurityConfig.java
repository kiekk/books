package com.thehecklers.aircraftpositions.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authz -> {
                    authz
                            .requestMatchers("/aircraftadmin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .formLogin(Customizer.withDefaults())
                .build();
    }

    @Bean
    UserDetailsService authentication(PasswordEncoder encoder) {
        String password = encoder.encode("password");
        UserDetails peter = User.builder()
                .username("peter")
                .password(password)
                .roles("USER")
                .build();

        UserDetails jodie = User.builder()
                .username("jodie")
                .password(password)
                .roles("USER", "ADMIN")
                .build();

        System.out.println("   >>> Peter's password : " + peter.getPassword());
        System.out.println("   >>> Jodie's password : " + jodie.getPassword());

        return new InMemoryUserDetailsManager(peter, jodie);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}