package io.lcalmsky.app.modules.account.endpoint.controller.form;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import static lombok.AccessLevel.PROTECTED;

@Data
@NoArgsConstructor
public class NicknameForm {

    @NotBlank
    @Length(min = 3, max = 20)
    @Pattern(regexp = "^[ㄱ-ㅎ가-힣a-z0-9-]{3,20}$")
    private String nickname;

    public NicknameForm(String nickname) {
        this.nickname = nickname;
    }
}
