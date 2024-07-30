package io.lcalmsky.app.modules.account.endpoint.controller;

import io.lcalmsky.app.infra.email.EmailMessage;
import io.lcalmsky.app.infra.email.EmailService;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository; // 회원 가입 이후 이메일 검증을 위해 추가
    @MockBean EmailService emailService;

    @Test
    @DisplayName("회원 가입 화면 진입 확인")
    public void singUpForm() throws Exception {
        mockMvc.perform(
                        get("/sign-up"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("account/sign-up"))
                .andExpect(model().attributeExists("signUpForm"));
    }

    @Test
    @DisplayName("회원 가입 처리: 입력값 오류")
    void signUpSubmitWithError() throws Exception {
        mockMvc.perform(post("/sign-up")
                        .param("nickname", "nickname")
                        .param("email", "email@gmail")//이메일을 일부러 포맷에 맞지 않게 입력
                        .param("password", "1234!") // 비밀번호를 일부러 8자리가 안 되도록 입력
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk()) // 상태는 처리 여부 상관없이 200 ok 를 반환
                //AccountController에 페이즈를 이동시키도록 구현되어 있다
                .andExpect(view().name("account/sign-up"));
                // 입력값이 잘못되었기 때문에 /sign-up 페이지로 되돌아가 에러
    }

    @DisplayName("회원 가입 처리: 입력값 정상")
    @Test
    void signUpSubmit() throws Exception {
        mockMvc.perform(post("/sign-up")
                        .param("nickname", "nickname")
                        .param("email", "dlatjdgur122@naver.com")
                        .param("password", "1234!@#$")
                        .with(csrf()))
                // 모든 필드의 값을 정상적으로 입력하고 csrf 설정. security, thymeleaf 를 같이 사용하면
                // thymeleaf 에서 csrf 토큰을 임의로 생성해서 넣어주기에 csrf() 없이 수행할 경우 403 에러 발생
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                // 모두 정상적으로 입력했을 경우 redirect 하도록 되어 해당 상태를 반환
                .andExpect(view().name("redirect:/"));
                // redirect 되어 루트 페이지로 이동했는지 확인

        assertTrue(accountRepository.existsByEmail("dlatjdgur122@naver.com"));
        Account account = accountRepository.findByEmail("dlatjdgur122@naver.com");
        // Account Entity 조회를 위해 AccountRepository에 findByEmail 메서드를 추가하고 조회
        assertNotEquals(account.getPassword(), "1234!@#$" );
        assertNotNull(account.getEmailToken());
        // 메일이 DB에 저장되었는지 확인
        // 조회한 Account Entity의 비밀번호와 실제 입력한 비밀번호가 다른지 검증, 비밀번호 인코딩이 수행됐다면 두 값이 서로 달라야 정상

        then(emailService)
                .should()
                .sendEmail(any(EmailMessage.class));
        // 메일이 전송했는지 확인. 실제로 전송 여부를 확인할기 어렵기에 JavaMailSender를 @MockBean을 이용해 주입하고
        // mailSender 가 send라는 메서드를 호출했고 그 때 전달된 타입이 SimpleMailMessage 타입인지 확인
    }

    @DisplayName("인증 메일 확인: 잘못된 링크")
    @Test
    void verifyEmailWithWrongLink() throws Exception {
        mockMvc.perform(get("/check-email-token")
                        .param("token", "token")
                        .param("email", "email"))
                        // 유효하지 않는 토큰과 이메일을 입력한다.
                .andExpect(status().isOk())
                .andExpect(view().name("account/email-verification"))
                // 상태 자체는 200 OK 에서 변함이 없고 view도 유지되어야 한다.
                .andExpect(model().attributeExists("error"))
                // error 객체가 model 객체를 통해 전달되어야 한다.
                .andExpect(unauthenticated()); // 인증 실패 결과를 기대할 때 사용
    }

    @DisplayName("인증 메일 확인: 유효한 링크")
    @Test
    @Transactional // DB 트랜잭션이 발생하기 때문에 사용
    void verifyEmail() throws Exception {
        Account account = Account.builder() // 토큰을 생성하고 DB와 비교해야 하기 때문에 생성
                .email("email@email.com")
                .password("1234!@#$")
                .nickname("nickname")
                .notificationSetting(Account.NotificationSetting.builder()
                        .studyCreatedByWeb(true)
                        .studyUpdatedByWeb(true)
                        .studyRegistrationResultByWeb(true)
                        .build())
                .build();
        Account newAccount = accountRepository.save(account); // 저장
        newAccount.generateToken(); // 토큰 생성
        mockMvc.perform(get("/check-email-token")
                        .param("token", newAccount.getEmailToken())
                        .param("email", newAccount.getEmail()))
                // 요청시 전달할 토큰과 이메일을 계정 생성시 사용한 것과 동일한 것으로 넣는다.
                .andExpect(status().isOk())
                .andExpect(view().name("account/email-verification"))
                // 상태와 view는 변홤이 없어야 한다.
                .andExpect(model().attributeDoesNotExist("error"))
                // error 객체가 포함되면 안된다.
                .andExpect(model().attributeExists("numberOfUsers", "nickname"))
                // numberOfUsers, nickname 이 model을 통해 전달되어야 한다.
                .andExpect(authenticated().withUsername("nickname"));
                // 인증 성공 후 어떤 username을 사용했는지까지 확인할 수 있다. 그 외에 다른 속성들도 확인할 수 있다
    }
}