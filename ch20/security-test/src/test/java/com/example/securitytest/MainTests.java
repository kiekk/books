package com.example.securitytest;

import com.example.securitytest.service.NameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc // 스프링 부트가 MockMvc를 자동 구성하게 합니다.
public class MainTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NameService nameService;

    @Test
    @DisplayName("인증되지 않은 사용자는 '/hello' 를 요청할 수 없다.")
    void hellUnauthenticated() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("인증된 사용자는 '/hello' 를 요청할 수 있다.")
    void helloAuthenticated() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(content().string("Hello!"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "mary", password = "12345", authorities = "read")
    @DisplayName("MockUser 세부 정보 설정")
    void helloAuthenticated2() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(content().string("Hello!"))
                .andExpect(status().isOk());
    }

    @Test
    void helloAuthenticatedWithUser() throws Exception {
        mockMvc.perform(
                        get("/hello").with(user("mary"))
                )
                .andExpect(content().string("Hello!"))
                .andExpect(status().isOk());
    }

    // UserDetailsService에서 john을 로드한다.
    // 이 경우 UserDetailsService 빈이 있어야 합니다.
    @Test
    @WithUserDetails("mary")
    void helloAuthenticatedWithUserDetails() throws Exception {
        mockMvc.perform(get("/hello"))
                .andExpect(content().string("Hello!"))
                .andExpect(status().isOk());
    }

    @Test
    @WithCustomUser(username = "mary")
    void helloAuthenticationWithCustomSecurityContextFactory() throws Exception {
        // 인증 논리를 건너뛰기 떄문에 권한 부여 및 인증 이후 처리 작업만 테스트 가능합니다.
        // 특정 사용자에 대한 테스트는 불가능
        mockMvc.perform(get("/hello"))
                .andExpect(content().string("Hello!"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("인증되지 않은 사용자는 AuthenticationException이 발생한다.")
    void testNameServiceWithNoUser() {
        assertThatThrownBy(() -> nameService.getName())
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    @WithMockUser(authorities = "read")
    @DisplayName("권한이 없는 사용자는 AccessDeniedException이 발생한다.")
    void testNameServiceWithUserButWrongAuthority() {
        assertThatThrownBy(() -> nameService.getName())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(authorities = "write")
    @DisplayName("권한이 있는 사용자는 요청에 성공합니다.")
    void testNameServiceWithUserButCorrectAuthority() {
        String result = nameService.getName();

        assertThat(result).isEqualTo("Fantastic");
    }

    @Test
    void helloAuthenticatedWithValidUser() throws Exception {
        mockMvc.perform(
                        get("/hello")
                                .with(httpBasic("john", "12345"))
                )
                .andExpect(content().string("Hello!"))
                .andExpect(status().isOk());
    }

    @Test
    void helloAuthenticatedWithInvalidUser() throws Exception {
        mockMvc.perform(
                        get("/hello")
                                .with(httpBasic("mary", "1234"))
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loggingInWithWrongUser() throws Exception {
        mockMvc.perform(formLogin()
                        .user("joey").password("12345"))
                .andExpect(header().exists("failed"))
                .andExpect(unauthenticated());
    }

    @Test
    void loggingInWithWrongAuthority() throws Exception {
        mockMvc.perform(formLogin()
                        .user("mary").password("12345"))
                .andExpect(redirectedUrl("/error"))
                .andExpect(status().isFound())
                .andExpect(authenticated());
    }

    @Test
    void loggingInWithCorrectAuthority() throws Exception {
        mockMvc.perform(formLogin()
                        .user("john").password("12345"))
                .andExpect(redirectedUrl("/home"))
                .andExpect(status().isFound())
                .andExpect(authenticated());
    }

    @Test
    @DisplayName("CSRF 토큰 없이 호출 시 403 Forbidden이 반환된다.")
    void testHelloPOST() throws Exception {
        mockMvc.perform(post("/hello"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF 토큰이 있으면 요청은 정상적으로 호출된다.")
    void testHelloPOSTWithCSRF() throws Exception {
        mockMvc.perform(
                        post("/hello")
                                .with(csrf())
                )
                .andExpect(status().isOk());
    }

}
