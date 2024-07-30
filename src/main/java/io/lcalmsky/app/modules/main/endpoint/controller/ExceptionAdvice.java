package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@ControllerAdvice
@Slf4j
public class ExceptionAdvice {

    @ExceptionHandler
    public String handleRuntimeException(@CurrentUser Account account, HttpServletRequest request, RuntimeException exception) {
        log.info(getNicknameIfExists(account) + "requested {}", request.getRequestURI());
        log.error("bad request", exception);
        return "error";
    }

    private String getNicknameIfExists(Account account) {
        return Optional.ofNullable(account)
                .map(Account::getNickname)
                .map(s -> s + " ")
                .orElse("");
    }
}
