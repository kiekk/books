package com.example.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

//이 클래스로부터 생성된 객체가 시큐리티 설정 파일임을 의미하면서 동시에 시큐리티를 사용하는데 필요한 객체들을 생성합니다.
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	//BoardUserDetailsService 사용으로 주석 처리
	//@Autowired
	//private DataSource dataSource;
	
	@Autowired
	private BoardUserDetailsService boardUserDetailsService;
	
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}
	
	/*
	 * authenticated() : 사용자 인증과 권한을 설정
	 * antMatchers(URL) : 매칭되는 URL 패턴들에 대한 접근 허용
	 * permitAll() : 모든 사용자에게 접근 허용
	 * hasRole(권한) : 특정 권한을 가진 사용자만 접근 허용
	 * 
	 * csrf() : 크로스 사이트 위조 요청에 대한 설정
	 * RESTfull을 사용하기 위해서는 이 csrf 기능을 비활성화해야 합니다.
	 */
	//HttpSecurity를 이용해 애플리케이션 자원에 대한 인증과 인가를 제어할 수 있습니다.
	@Override
	protected void configure(HttpSecurity security) throws Exception {
		security.authorizeRequests().antMatchers("/").permitAll();
		security.authorizeRequests().antMatchers("/member/**").authenticated();
		security.authorizeRequests().antMatchers("/manager/**").hasRole("MANAGER");
		security.authorizeRequests().antMatchers("/admin/**").hasRole("ADMIN");
		
		//RESTfull 사용을 위한 설정
		security.csrf().disable();
		
		//사용자 인증을 위한 로그인 화면 제공
		//스프링 부트가 제공하는 로그인 화면 제공
		//security.formLogin(); 
		
		//사용자가 직접 작성한 로그인 화면 제공
		security.formLogin().loginPage("/login").defaultSuccessUrl("/loginSuccess", true);
		
		//접근 권한이 없을 때 보여줄 페이지
		security.exceptionHandling().accessDeniedPage("/accessDenied");
		
		//로그아웃 기능 추가
		security.logout().invalidateHttpSession(true).logoutSuccessUrl("/login");
		
		//authenticate() 대신
		security.userDetailsService(boardUserDetailsService);
	}
	
	/*
	 * 실제 데이터베이스에 저장된 회원 정보로 인증, 인가 확인하기
	 * 
	 * query1 : 인증 확인
	 * query2 : 인가 확인
	 * 
	 * BoardUserDetailsService 사용으로 주석 처리
	 */
//	@Autowired
//	public void authenticate(AuthenticationManagerBuilder auth) throws Exception {
//		String query1 = "select id username, concat('{noop}', password) " 
//						+ "password, true enabled from member where id=?";
//		
//		String query2 = "select id, role from member where id=?";
//		
//		auth.jdbcAuthentication()
//			.dataSource(dataSource)
//			.usersByUsernameQuery(query1)
//			.authoritiesByUsernameQuery(query2);
//	}
	
	//임의로 아이디, 비밀번호를 설정하여 인증, 인가 확인
//	@Autowired
//	public void authenticate(AuthenticationManagerBuilder auth) throws Exception {
//		auth.inMemoryAuthentication()
//			.withUser("manager")
//			.password("{noop}manager123")
//			.roles("MANAGER");
//		
//		auth.inMemoryAuthentication()
//			.withUser("admin")
//			.password("{noop}admin123")
//			.roles("ADMIN");
//	}
}
