package io.lcalmsky.app.modules.account.endpoint.controller.validator;

import io.lcalmsky.app.modules.account.endpoint.controller.form.PasswordForm;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class PasswordFormValidator implements Validator { //Validator를 구현
    @Override
    public boolean supports(Class<?> clazz) { // 어떤 타입에 대해 validate 할지 결정한다.
        return PasswordForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) { // 위 코드에서 PasswordForm 타입에 할당할 수 있는 타입만 받도록
        // 하였기에 target 객체는 passwordForm으로 캐시팅 할 수 있다. 그 이후 새로운 비밀번호와 비밀번호 확인이 동일한지 체크하여
        // 동일하지 않을 경우 에러 객체에 에러 문구를 전달
        PasswordForm passwordForm = (PasswordForm) target;
        if (!passwordForm.getNewPassword().equals(passwordForm.getNewPasswordConfirm())) {
            errors.rejectValue("newPassword", "wrong.value", "입력한 새 패스워드가 일치하지 않습니다.");
        }

    }
}
