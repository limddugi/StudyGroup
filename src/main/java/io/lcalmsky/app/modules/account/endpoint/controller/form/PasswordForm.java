package io.lcalmsky.app.modules.account.endpoint.controller.form;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import static lombok.AccessLevel.*;

@Data
@NoArgsConstructor
public class PasswordForm {
    @Length(min = 8, max = 50)
    private String newPassword;
    @Length(min = 8, max = 50)
    private String newPasswordConfirm;
}
