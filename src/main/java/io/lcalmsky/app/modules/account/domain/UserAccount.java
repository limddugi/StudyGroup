package io.lcalmsky.app.modules.account.domain;


import io.lcalmsky.app.modules.account.domain.entity.Account;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class UserAccount extends User {

    private final Account account; //@CurrentUser 어노테이션에서 account 를 반환하도록 하였기에 변수 이름을 반드시 account로 설정

    public UserAccount(Account account) {
        super(account.getNickname(), account.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_USER")));
        // User 객체를 생성하기 위해 username , password, authorities 가 필요하다. 우리가 사용하는 객체 account 에서 각각 추출
        // ( 권한은 기존 AccountService 에서 사용하던 것으로 동일하게 넣어준다. )
        this.account = account;
    }
}
