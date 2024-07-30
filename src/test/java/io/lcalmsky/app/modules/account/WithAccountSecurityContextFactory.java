package io.lcalmsky.app.modules.account;

import io.lcalmsky.app.modules.account.application.AccountService;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithAccountSecurityContextFactory implements WithSecurityContextFactory<WithAccount> {
    // WithSecurityContextFactory 인터페이스를 구현해야하고 이 때 전달할 타입은 이전에 생성한 어노테이션과 동일해야한다.
    private final AccountService accountService;

    public WithAccountSecurityContextFactory(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public SecurityContext createSecurityContext(WithAccount annotation) {
        // SecurityContext를 생성하기 위한 메서드를 구현
        String[] nicknames = annotation.value(); // @With 어노테이션의 attribute로 주입받은 nickname을 사용
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        for (String nickname : nicknames) {
            SignUpForm signUpForm = new SignUpForm();
            signUpForm.setNickname(nickname);
            signUpForm.setEmail(nickname + "@gamil.com");
            signUpForm.setPassword("1234asdf");
            accountService.signUp(signUpForm);

            UserDetails principal = accountService.loadUserByUsername(nickname);
            //가입 후 DB에 저장된 정보를 불러온다.
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
            // Authentication 구현체 중 하나인 토큰 객체를 생성해 DB에서 읽어온 값으로 설정
            context.setAuthentication(authentication);

        }
        return context;
    }
}
