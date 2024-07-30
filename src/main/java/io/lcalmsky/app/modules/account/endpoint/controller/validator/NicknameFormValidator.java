package io.lcalmsky.app.modules.account.endpoint.controller.validator;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.form.NicknameForm;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component // AccountRepository를 주입받기 위해 컴포넌트로 등록
@RequiredArgsConstructor
public class NicknameFormValidator implements Validator {

    private final AccountRepository accountRepository; // 계정 정보를 조회하기 위해 AccountRepository를 주입

    @Override
    public boolean supports(Class<?> clazz) {
        return NicknameForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) { // DB에 동일한 nickname을 가진 계정이 있는지 확인하여 존재하는 경우
                                                            // 에러 객체로 에러 문구로 전달
        NicknameForm nicknameForm = (NicknameForm) target;
        Account account = accountRepository.findByNickname(nicknameForm.getNickname());
        if (account != null) {
            errors.rejectValue("nickname", "wrong.value", "이미 사용중인 닉네임입니다.");
        }

    }
}
