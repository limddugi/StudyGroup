package io.lcalmsky.app.modules.account.application;

import io.lcalmsky.app.infra.config.AppProperties;
import io.lcalmsky.app.infra.email.EmailMessage;
import io.lcalmsky.app.infra.email.EmailService;
import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.account.endpoint.controller.form.NotificationForm;
import io.lcalmsky.app.modules.account.endpoint.controller.form.Profile;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AccountService implements UserDetailsService {
    // AccountService가 UserDetailsService를 구현하게 된다. UserDetailsService의 구현체가 존재하고
    // 구현체가 Bean으로 등록되어 있을 경우 spring security 설정을 추가로 수정할 필요가 없다

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder; // 인코더 빈 주입
    private final EmailService emailService;
    private final TemplateEngine templateEngine; // HTML 메시지를 생성하기 위해 주입
    private final AppProperties appProperties; // 호스트 정보를 획득하기 위해 주입


    public Account signUp(SignUpForm signUpForm) {
        Account newAccount = seveNewAccount(signUpForm);
        sendVerificationEmail(newAccount);
        return newAccount; //signUp 메서드가 새로 생성해서 account를 반환
    }

    private Account seveNewAccount(SignUpForm signUpForm) {
        Account account =
                Account.with(signUpForm.getEmail(), signUpForm.getNickname(), passwordEncoder.encode(signUpForm.getPassword()));
        // static 생성자를 이용해 객체를 생성한다. builder를 사용할 경우 클래스 내에서 설정한 기본 값이 동작하지 않으므로 객체를 생성하는 방식으로 수정
        account.generateToken();
        return accountRepository.save(account);
        // 토큰을 생성하는 부분으로 기존 위치에 있을 경우 업데이트 쿼리가 두 번 발생하지만 이 위치에 있을 경우 한 번 발생한다.
    }


    public Account findAccountByEmail(String email) {
        return accountRepository.findByEmail(email); // Account Entity 를 가져옴
    }


    private Account saveNewAccount(SignUpForm signUpForm) {
        Account account = Account.builder()
                .email(signUpForm.getEmail())
                .nickname(signUpForm.getNickname())
                .password(passwordEncoder.encode(signUpForm.getPassword())) // 비밀번호를 인코딩한 뒤 저장
                .notificationSetting(Account.NotificationSetting.builder() // 알림 설정
                        .studyCreatedByWeb(true)
                        .studyUpdatedByWeb(true)
                        .studyRegistrationResultByWeb(true)
                        .build())
                .build();
        return accountRepository.save(account);
    }

    public void sendVerificationEmail(Account newAccount) { // (3)
        Context context = new Context();
        context.setVariable("link", String.format("/check-email-token?token=%s&email=%s", newAccount.getEmailToken(),
                newAccount.getEmail()));
        context.setVariable("nickname", newAccount.getNickname());
        context.setVariable("linkName", "이메일 인증하기");
        context.setVariable("message", "FRITZ 가입 인증을 위해 링크를 클릭하세요.");
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        emailService.sendEmail(EmailMessage.builder()
                .to(newAccount.getEmail())
                .subject("FRITZ 회원 가입 인증")
                .message(message)
                .build());
    }

    public void login(Account account) {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(new UserAccount(account), // 변경
                account.getPassword(), Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(token);  //AuthenticationManager를 쓰는 방법이 정석적인 방법

        //SecurityContextHolder.getContext() 로 SecurityContext를 얻는다. 전역에서 호출할 수 있고 하나의 Context 객체가 반환
        //setAuthentication 을 이용해 인증 토큰을 전달할 수 있는데 이 떄 전달해야 할 토큰이 UsernamePasswordAuthenticationToken이다
        //UsernamePasswordAuthenticationToken 생성자로 nickname, password, Role을 각각 전달
        //Role 은 인가(권한) 개념으로 이 계정의 사용자가 어떤 기능까지 사용할 수 있는지를 나타낼 수 있고 현재 사용자로 정의했다
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // UserDetailsService가 제공하는 인터페이스를 재정의 한다. 회원 정보를 DB로 관리하는지, 메모리로 관리하는지, 파일로 관리하는지
        // 알지 못하기 때문에 사용자가 존재하는지 확인하여 사용자 정보를 반환해주면 나머지는 spring security가 알아서 처리

        Account account = Optional.ofNullable(accountRepository.findByEmail(username))
                // 이메일 또는 닉네임이 존재하는지 확인해야 하기에 두 가지 정보를 모두 확인
                .orElse(accountRepository.findByNickname(username));
        if (account == null) { // 둘다 확인했을 때도 계정이 검색되지 않는 경우 메서드 시그니처에서 가이드하고 있는
            //UsernameNotFoundException 을 규격 (username을 생성자로 전달)에 맞게 생성하여 던져준다.
            throw new UsernameNotFoundException(username);
        }
        return new UserAccount(account); // 계정이 존재할 경우 UserDetails 인터페이스 구현체를 반환한다. UserAccount 클래스가
        // UserDetails 인터페이스를 구현하게 했으므로 해당 객체를 반환하면 된다.
    }

    public void verified(Account account) {
        account.verified();
        login(account);

    }

    public void updateProfile(Account account, Profile profile) {
        account.updateProfile(profile);
        accountRepository.save(account); // 수정한 정보를 Repository를 통해 저장
    }

    public void updatePassword(Account account, String newPassword) {
        account.updatePassword(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    public void updateNotification(Account account, NotificationForm notificationForm) {
        account.updateNotification(notificationForm);
        accountRepository.save(account);
    }

    public void updateNickname(Account account, String nickname) {
        account.updateNickname(nickname);
        accountRepository.save(account);
        login(account); // 증요!!!
    }

    public void sendLoginLink(Account account) { // (4)
        Context context = new Context();
        context.setVariable("link", "/login-by-email?token=" + account.getEmailToken() + "&email=" + account.getEmail());
        context.setVariable("nickname", account.getNickname());
        context.setVariable("linkName", "FRITZ 로그인하기");
        context.setVariable("message", "로그인 하려면 아래 링크를 클릭하세요.");
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        account.generateToken();
        emailService.sendEmail(EmailMessage.builder()
                .to(account.getEmail())
                .subject("[FRITZ] 로그인 링크")
                .message(message)
                .build());
    }

    public void addTag(Account account, Tag tag) {
        // 계정 정보를 먼저 찾은 뒤 계정이 존재하면 태그를 추가
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getTags().add(tag));
    }

    public Set<Tag> getTags(Account account) {
        // 계정 정보를 찾은 뒤 계정 정보가 존재하지 않으면 예외를 던지고, 존재하면 계정이 가진 태그를 반환
        return accountRepository.findById(account.getId()).orElseThrow().getTags();
    }

    public void removeTag(Account account, Tag tag) {
        // 계정 정보를 찾은 뒤 계정 정보가 존재하면 그 계정을 가지는 태그 정보를 가져와 전달한 태그를 삭제
         accountRepository.findById(account.getId())
                .map(Account::getTags)
                .ifPresent(tags -> tags.remove(tag));

    }

    public Set<Zone> getZones(Account account) {
        return accountRepository.findById(account.getId())
                .orElseThrow()
                .getZones();
    }

    public void addZone(Account account, Zone zone) {
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getZones().add(zone));
    }

    public void removeZone(Account account, Zone zone) {
        accountRepository.findById(account.getId())
                .ifPresent(a -> a.getZones().remove(zone));
    }

    public Account getAccountBy(String nickname) {
        return Optional.ofNullable(accountRepository.findByNickname(nickname))
                .orElseThrow(() -> new IllegalArgumentException(nickname + "에 해당하는 사용자가 없습니다."));
    }
}
