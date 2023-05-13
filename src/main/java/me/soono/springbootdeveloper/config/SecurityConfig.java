package me.soono.springbootdeveloper.config;


import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.service.user.UserDetailService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;
import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toStaticResources;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authz -> {
                    // security warning 제거
                    // You are asking Spring Security to ignore Mvc [pattern='/static/**'].
                    // This is not recommended -- please use permitAll via HttpSecurity#authorizeHttpRequests instead.
                    authz.requestMatchers(toH2Console(), toStaticResources().atCommonLocations()).permitAll();
                    authz.requestMatchers("/login", "/signup", "/user").permitAll();
                    authz.anyRequest().authenticated();
                })
                .formLogin(formLogin -> {
                    formLogin.usernameParameter("email");
                    formLogin.loginPage("/login");
                    formLogin.defaultSuccessUrl("/articles");
                })
                .logout(logout -> {
                    logout.logoutSuccessUrl("/login");
                    logout.invalidateHttpSession(true);
                })
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       BCryptPasswordEncoder bCryptPasswordEncoder,
                                                       UserDetailService userDetailService) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailService)
                .passwordEncoder(bCryptPasswordEncoder)
                .and().build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
