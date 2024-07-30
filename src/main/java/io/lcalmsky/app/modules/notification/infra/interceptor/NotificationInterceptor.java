package io.lcalmsky.app.modules.notification.infra.interceptor;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationInterceptor implements HandlerInterceptor {
    private final NotificationRepository notificationRepository;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (modelAndView != null && !isRedirectView(modelAndView)
                // 리다이렉트가 아니고
                && authentication != null && isTypeOfUserAccount(authentication)) {
            // 인증 정보가 존재하고 UserAccount타입일떄
            Account account = ((UserAccount) authentication.getPrincipal()).getAccount();
            // Account 정보를 획득하여
            long count = notificationRepository.countByAccountAndChecked(account, false);
            // 알림 정보를 조회하고
            modelAndView.addObject("hasNotification", count > 0);
            //Model로 전달한다.
        }
    }

    private boolean isRedirectView(ModelAndView modelAndView) {
        Optional<ModelAndView> optionalModelAndView = Optional.ofNullable(modelAndView);
        return startWithRedirect(optionalModelAndView) || isTypeOfRedirectView(optionalModelAndView);
    }

    private Boolean startWithRedirect(Optional<ModelAndView> optionalModelAndView) {
        return optionalModelAndView.map(ModelAndView::getViewName)
                .map(viewName -> viewName.startsWith("redirect:"))
                .orElse(false);
    }

    private Boolean isTypeOfRedirectView(Optional<ModelAndView> optionalModelAndView) {
        return optionalModelAndView.map(ModelAndView::getView)
                .map(v -> v instanceof RedirectView)
                .orElse(false);
    }

    private boolean isTypeOfUserAccount(Authentication authentication) {
        return authentication.getPrincipal() instanceof
                UserAccount;
    }
}
