package tacos.security;

import javax.annotation.security.PermitAll;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	DataSource dataSource;
	
	@Autowired
	private UserDetailsService userDetailsService;
	
	@Bean
	public PasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.authorizeRequests()
			.antMatchers("/design", "/orders")
			.access("hasRole('ROLE_USER')")
			.antMatchers("/", "/**").permitAll()
			.and()
			.formLogin()
			.loginPage("/login")
			.and()
			.logout()
			.logoutSuccessUrl("/")
			.and()
			.csrf();
	}
	
//	@Override
//	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//		/*
//		auth.inMemoryAuthentication()
//			.withUser("user1")
//			.password("{noop}password1")
//			.authorities("ROLE_USER")
//			.and()
//			.withUser("user2")
//			.password("{noop}password2")
//			.authorities("ROLE_USER");
//		*/
//		//jdbc를 이요한 사용자 스토어 구현
//		auth
//		.jdbcAuthentication()
//		.dataSource(dataSource)
//		.usersByUsernameQuery(
//				"select username, password, enabled from users where username=?")
//		.authoritiesByUsernameQuery(
//				"select username, authority from authorities where username=?")
//		//.passwordEncoder(new BCryptPasswordEncoder());	//bcrypt를 해싱 암호화합니다.
//		.passwordEncoder(new NoEncodingPasswordEncoder());	//테스트용, 비밀번호를 암호화하지 않은 상태에서 비교
//	}
	
	//LDAP 기반 인증으로 스프링 시큐리티 구성
//	@Override
//	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//		auth
//			.ldapAuthentication()
//			.userSearchBase("ou=people")
//			.userSearchFilter("(uid={0})")
//			.groupSearchBase("ou=groups")
//			.groupSearchFilter("member={0}")
//			.contextSource()
//			.root("dc=tacocloud,dc=com")
//			.and()
//			.passwordCompare()
//			.passwordEncoder(new BCryptPasswordEncoder())
//			.passwordAttribute("userPasscode");
//	}
	
	//Service로 스프링 시큐리티 구성
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth
		.userDetailsService(userDetailsService)
		.passwordEncoder(encoder());
	}
}
