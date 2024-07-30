package io.lcalmsky.app.modules.account.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.form.SignUpForm;
import io.lcalmsky.app.modules.account.endpoint.controller.validator.SignUpFormValidator;
import io.lcalmsky.app.modules.account.application.AccountService;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final SignUpFormValidator signUpFormValidator;
    private final AccountService accountService;
    private final AccountRepository accountRepository;


    @InitBinder("signUpForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(signUpFormValidator);
    }

    @GetMapping("/sign-up")
    public String signUpForm(Model model) {
        model.addAttribute(new SignUpForm());
        return "account/sign-up";
    }

    @PostMapping("/sign-up")
    public String signUpSubmit(@Valid @ModelAttribute SignUpForm signUpForm, Errors errors) {
        if (errors.hasErrors()) {
            // 에러가 존재할 경우 다시 회원가입 페이지를 띄운다. Errors 객체로 에러가 전달되기 때문에
            // Thymeleaf로 렌더링 된 HTML에 해당 에러를 전달해 업데이트 할 수 있다.
            return "account/sign-up";
        }
        Account account = accountService.signUp(signUpForm);
        accountService.login(account);
        // TODO: 회원 가입 처리
        return "redirect:/";
    }

    @GetMapping("/check-email-token")
    public String verifyEmail(String token, String email, Model model) {
        // 이메일 링크를 클릭하면 해당 메서드로 진입하게 되고 그 때 email, token을 파라미터로 전달
        Account account = accountService.findAccountByEmail(email); // 계정 정보를 가져오도록 위임
        if (account == null) { // 계정정보가 없으면 기존에 가입한 사용자가 아니므로 모델 객체에 에러를 전달
            model.addAttribute("error", "wrong.email");
            return "account/email-verification";
        }
        if (!token.equals(account.getEmailToken())) { // 계정정보가 있지만 기존에 발급한 token과 일치하지 않는 경우 에러를 전달
            model.addAttribute("error", "wrong.token");
            return "account/email-verification";
        }
        accountService.verified(account); // email, token이 모두 유효하므로 인증 완료 처리를 한다
        accountService.login(account);
        model.addAttribute("numberOfUsers", accountRepository.count());
        model.addAttribute("nickname", account.getNickname());
        // 인증에 성공 후 보여줄 데이터를 모델 객체에 전달
        return "account/email-verification"; // 이메일 인증 화면으로 리다이렉트
    }

    @GetMapping("/check-email")
    public String checkMail(@CurrentUser Account account, Model model) {
        // 가입한 이후 내비게이션 바 경고창을 클릭했을 때 이동하므로 가입할 때 사용한 email 정보를 넘겨주면서 리다이렉트
        model.addAttribute("email", account.getEmail());
        return "account/check-email";
    }

    @GetMapping("/resend-email")
    public String resendEmail(@CurrentUser Account account, Model model) {
        // 이메일 재전송할 때 호출되는 부분으로 새로로침이나 악용하지 못하도록 5분에 한 번만 메일을 보낼 수 있도록 방어 로직을 추가한다.
        // 인증 메일을 보낼 수 있는 시간이 되면 방어로직은 통과하게 되고 이메일을 보내는 메서드가 실행
        if (!account.enableToSendEmail()) {
            model.addAttribute("error", "인증 이메일은 5분에 한 번만 전송할 수 있습니다.");
            model.addAttribute("email", account.getEmail());
            return "account/check-email";
        }
        accountService.sendVerificationEmail(account);
        return "redirect:/";
    }

    @GetMapping("/profile/{nickname}")
    public String viewProfile(@PathVariable String nickname, Model model, @CurrentUser Account account) {
        Account accountByNickname = accountService.getAccountBy(nickname);
        model.addAttribute(accountByNickname); // 키를 생략하면 객체 타입을 camel-case로 전달
        model.addAttribute("isOwner", accountByNickname.equals(account)); // 전달된 객체와 DB에서 조회한 객체가 같으면 인증된 사용자
        return "account/profile";
    }

    @GetMapping("/email-login")
    public String emailLoginForm() { // 이메일 로그인 뷰 페이지로 라우팅한다.
        return "account/email-login";
    }

    @PostMapping("/email-login")
    public String sendLinkForEmailLogin(String email, Model model, RedirectAttributes attributes) {
        //이메일 폼을 통해 입력받은 정보로 계정을 찾아 메일을 전송하고 다시 리다이렉트한다. 계정이 존재하지 않을 경우 에러를 전달

        Account account = accountRepository.findByEmail(email);
        if (account == null) {
            model.addAttribute("error", "유효한 이메일 주소가 아닙니다.");
            return "account/email-login";
        }
//        if (!account.enableToSendEmail()) {
//            model.addAttribute("error", "너무 잦은 요청입니다. 5분 뒤에 다시 시도하세요.");
//            return "account/email-login";
//        }
        accountService.sendLoginLink(account);
        attributes.addFlashAttribute("message", "로그인 가능한 링크를 이메일로 전송하였습니다.");
        return "redirect:/email-login";
    }

    @GetMapping("/login-by-email")
    public String loginByEmail(String token, String email, Model model) {
        //링크를 통해 전달한 토큰과 이메일 정보를 가지고 토큰의 유효성을 판단하고 유효한 경우 로그인을 수행해 인증정보를 업데이트 하고
        //페이지를 이동한다. 토큰이나 이메일이 유효하지 않을 경우 에러를 전달.

        Account account = accountRepository.findByEmail(email);
        if (account == null || !account.isValid(token)) {
            model.addAttribute("error", "로그인할 수 없습니다.");
            return "account/logged-in-by-email";
        }
        accountService.login(account);
        return "account/logged-in-by-email";
    }
}
