package com.example.recipe741.board.security;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class TodoSecurityConfig extends WebSecurityConfigurerAdapter {

    // 접근 통제 결정 관리자를 따로 등록하지 않으면 아래와 같이 기본 접근 통제 결정 관리자를자동 구성합니다.
//    @Bean
//    public AffirmativeBased accessDecisionManager() {
//        return new AffirmativeBased(Arrays.asList(new RoleVoter(), new AuthenticatedVoter()));
//    }

    // 커스텀 결정 관리자 사용
    @Bean
    public AffirmativeBased accessDecisionManager() {
        return new AffirmativeBased(Arrays.asList(new RoleVoter(), new AuthenticatedVoter(), new IpAddressVoter()));
    }

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("board")
                .addScript("classpath:/schema.sql")
                .addScript("classpath:/data.sql")
                .build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth
            .jdbcAuthentication()
                .passwordEncoder(passwordEncoder())
                .dataSource(dataSource());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.authorizeRequests()
        // 접근 통제 결정 관리자를 따로 등록하지 않으면 아래와 같이 기본 접근 통제 결정 관리자를자동 구성합니다.
//                .accessDecisionManager(accessDecisionManager())
                .accessDecisionManager(accessDecisionManager())
                .antMatchers("/todos*").hasAuthority("USER")
                .antMatchers(HttpMethod.DELETE, "/todos*").hasAuthority("ADMIN")
                // url에 한해 접근 속성 부여
//                .antMatchers(HttpMethod.DELETE, "/todos*").access("ADMIN,IP_LOCAL_HOST")
            .and()
                .httpBasic().disable()
                .formLogin()
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .failureUrl("/login?error=true")
                    .permitAll()
            .and()
                .logout().logoutSuccessUrl("/logout-success");
    }



}
