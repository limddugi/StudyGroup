package io.lcalmsky.app.modules.account.support;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) //Runtime시 유지되어야 한다.
@Target(ElementType.PARAMETER) // 파라미터에 사용할 수 있어야 한다.
@AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : account")
// spEL을 이용하여 인증정보가 존재하지 않으면 null을 , 존재하면 account 라는 property를 반환합니다.
public @interface CurrentUser {
}
