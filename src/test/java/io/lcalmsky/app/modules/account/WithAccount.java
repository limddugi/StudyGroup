package io.lcalmsky.app.modules.account;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME) // 런타임시 동작하도록 설정
@WithSecurityContext(factory = WithAccountSecurityContextFactory.class)
// SecurityContext를 설정해줄 클래스 지정
public @interface WithAccount {
    String[] value() default""; // 하나의 값을 attribute 로 전달 받기위해 메서드를 명시. nickname 을 주입 받을 예정
}
