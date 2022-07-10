package tacos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authz) -> authz.antMatchers("/design", "/orders").hasRole("USER")
                        .antMatchers("/**").permitAll()).httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager() {
        // 개발시에만 withDefaultPasswordEncoder 사용을 권장
        // 실제 프로덕션(배포)용에는 deprecated

        // 여러 user 정보를 설정하려면 각각 UserDetails 객체 생성 후
        // InMemoryUserDetailsManager 생성 시 전부 전달하면 됨

        /*
            InMemoryUserDetailsManager 생성자는 UserDetails 를 여러 개 받을 수 있도록 되어 있음
            public InMemoryUserDetailsManager(UserDetails... users) {
            ...
            }
         */
        UserDetails user1 = User.withDefaultPasswordEncoder()
                .username("user1")
                .password("1234")
                .roles("USER")
                .build();
        UserDetails user2 = User.withDefaultPasswordEncoder()
                .username("user2")
                .password("1234")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user1, user2);
    }

}
