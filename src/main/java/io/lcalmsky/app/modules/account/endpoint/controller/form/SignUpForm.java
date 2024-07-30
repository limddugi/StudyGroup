package io.lcalmsky.app.modules.account.endpoint.controller.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class SignUpForm {

    @NotBlank // 비어있는 값인지 여부를 검사
    @Length(min = 3, max = 20) // 문자열의 길이를 검사
    @Pattern(regexp = "^[ㄱ-ㅎ가-힣a-z0-9_-]{3,20}$") // 문자열 패턴을 검사, 정규표현식을 사용할 수 있다
    // 해당 정규표현식은 한글, 영어, 숫자, 언더스코어, 하이픈을 포함할 수 있다는 뜻
    private String nickname;

    @Email // 이메일 포맷인지 검사
    @NotBlank
    private String email;

    @NotBlank
    @Length(min = 8, max = 50)
    private String password;
}
