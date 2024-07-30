package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.application.AccountService;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MainControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountService accountService;
    @Autowired AccountRepository accountRepository;

    @BeforeEach
    void beforeEach() {
        SignUpForm signUpForm = new SignUpForm();
        signUpForm.setNickname("vzxcv");
        signUpForm.setEmail("vzxcv@123.com");
        signUpForm.setPassword("test123");
        accountService.signUp(signUpForm);
    }

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();

    }

    @DisplayName("이메일로 로그인: 성공")
    @Test
    void login_with_email() throws Exception {
        mockMvc.perform(post("/login") // /login을 호출 , parmeter로 username과 password를 전달
                        .param("username", "vzxcv@123.com")
                        .param("password", "test123")
                        .with(csrf())) // 스프링 시큐리티를 사용했기에 csrf 요청이 필요
                .andExpect(status().is3xxRedirection()) // 결과는 로그인 된 이후 redirect 응답을 받아야 한다.
                .andExpect(redirectedUrl("/")) // redirect된 url은 루트 ("/")가 되어야 한다.
                .andExpect(authenticated().withUsername("vzxcv"));
                // 인증이 되어야 하고 이 때 username 은 nickname이 되어야 한다.
                // 그 이유는 UserAccount 클래스에서 부모클래스의 생성자를 호출할 때 이메일이 아닌 nickname을 전달했기 떄문이다.
    }

    @Test
    @DisplayName("닉네임으로 로그인: 성공")
    void login_with_nickname() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "vzxcv")
                        .param("password", "test123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("vzxcv"));
    }

    @DisplayName("로그인 실패")
    @Test
    void login_fail() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", "test") // 가입할 때 사용하지 않은 nickname을 적는다
                        .param("password", "test1234")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection()) // 실패시에도 redirect 된다.
                .andExpect(redirectedUrl("/login?error")) // redirect 되는 url은 /login?error 이다.
                                    // 이는 spring security에서 자동으로 처리해주는 부분이다.
                .andExpect(unauthenticated()); // 로그인이 실패했기에 인증되지 않은 상태로 남아있다.
    }

    @DisplayName("로그아웃: 성공")
    @Test
    void logout() throws Exception{
        mockMvc.perform(post("/logout") // logout 요청을 한다
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/")) // logout 이후에 루트("/")로 redirect 되어야 한다.
                .andExpect(unauthenticated()); // logout 하였기에 인증되지 않은 상태가 되어야 한다,

    }
}