package me.soono.springbootdeveloper.config;

import lombok.RequiredArgsConstructor;
import me.soono.springbootdeveloper.config.oauth.OAuth2AuthorizationRequestBasedOnCookieRepository;
import me.soono.springbootdeveloper.config.oauth.OAuth2SuccessHandler;
import me.soono.springbootdeveloper.config.oauth.Oauth2UserCustomService;
import me.soono.springbootdeveloper.repository.tokoen.RefreshTokenRepository;
import me.soono.springbootdeveloper.service.user.UserDetailService;
import me.soono.springbootdeveloper.service.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;
import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toStaticResources;

@Configuration
@RequiredArgsConstructor
public class OauthSecurityConfig {

    private final Oauth2UserCustomService oauth2UserCustomService;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(toH2Console()))
                .headers(headers -> headers.frameOptions().disable())
                .authorizeHttpRequests(authz -> {
                    // security warning 제거
                    // You are asking Spring Security to ignore Mvc [pattern='/static/**'].
                    // This is not recommended -- please use permitAll via HttpSecurity#authorizeHttpRequests instead.
                    authz.requestMatchers(toH2Console(), toStaticResources().atCommonLocations()).permitAll();
                    authz.requestMatchers("/api/token").permitAll();
                    authz.requestMatchers("/api/**").authenticated();
                    authz.anyRequest().permitAll();
                })
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> {
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> {
                    oauth2.loginPage("/login");
                    oauth2.authorizationEndpoint(endpoint -> {
                        endpoint.authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository());
                    });
                    oauth2.successHandler(oAuth2SuccessHandler());
                    oauth2.userInfoEndpoint(endpoint -> {
                        endpoint.userService(oauth2UserCustomService);
                    });
                })
                .logout(logout -> {
                    logout.logoutSuccessUrl("/login");
                })
                .exceptionHandling(exception -> {
                    exception.defaultAuthenticationEntryPointFor(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                            new AntPathRequestMatcher("/api/**"));
                })
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

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }

    @Bean
    public OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository() {
        return new OAuth2AuthorizationRequestBasedOnCookieRepository();
    }

    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler(tokenProvider,
                refreshTokenRepository,
                oAuth2AuthorizationRequestBasedOnCookieRepository(),
                userService
        );
    }
}
