package com.example.recipe722.board.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
public class TodoSecurityConfig extends WebSecurityConfigurerAdapter {

    public TodoSecurityConfig() {
        super(true);
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("user").password("{noop}user").authorities("USER")
                .and()
                .withUser("admin").password("{noop}admin").authorities("USER", "ADMIN");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.authorizeRequests()
                .antMatchers("/todos*").hasAuthority("USER")
                .antMatchers(HttpMethod.DELETE, "/todos*").hasAuthority("ADMIN")
            .and()
                .anonymous()
            .and()
                .servletApi()
            .and()
                .securityContext()
            .and()
                .exceptionHandling()
            .and()
                .rememberMe()
            .and()
                .formLogin()
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .failureUrl("/login?error=true")
                    .defaultSuccessUrl("/todos")
                .permitAll()
            .and()
                .logout().logoutSuccessUrl("/logout-success")
            .and()
                .headers()
                // 로그아웃 후 '뒤로가기' 하면 로그인된 페이지 접속 가능해짐 (브라우저가 페이지를 캐시하기 때문)
                // 브라우저가 페이지를 캐시하지 않도록 설정
            .and()
                .csrf();
    }
}
