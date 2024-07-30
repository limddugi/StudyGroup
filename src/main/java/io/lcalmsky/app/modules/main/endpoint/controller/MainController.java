package io.lcalmsky.app.modules.main.endpoint.controller;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.event.infra.repository.EnrollmentRepository;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repostiory.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final StudyRepository studyRepository;
    private final AccountRepository accountRepository;
    private final EnrollmentRepository enrollmentRepository;

    @GetMapping("/")
    public String home(@CurrentUser Account account, Model model) {
        // @CurrentUser의 영향을 받아 현재 인증된 사용자 정보에 따라 객체가 할당
        if (account != null) {
            Account accountLoaded = accountRepository.findAccountWithTagsAndZonesById(account.getId());
            model.addAttribute(accountLoaded);
            model.addAttribute("enrollmentList",
                    enrollmentRepository.findByAccountAndAcceptedOrderByEnrolledAtDesc(accountLoaded, true));
            model.addAttribute("studyList",
                    studyRepository.findByAccount(accountLoaded.getTags(), accountLoaded.getZones()));
            model.addAttribute("studyManagerOf",
                    studyRepository.findFirst5ByManagersContainingAndClosedOrderByPublishedDateTimeDesc(
                            account, false));
            model.addAttribute("studyMemberOf",
                    studyRepository.findFirst5ByMembersContainingAndClosedOrderByPublishedDateTimeDesc(
                            account, false));
            return "home";
        }
        model.addAttribute
                ("studyList", studyRepository.findFirst9ByPublishedAndClosedOrderByPublishedDateTimeDesc(true, false));
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/search/study")
    public String searchStudy(String keyword, Model model,
                              @PageableDefault(size = 9, sort = "publishedDateTime", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Study> studyPage = studyRepository.findByKeyword(keyword, pageable);
        model.addAttribute("studyPage", studyPage);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sortProperty", pageable.getSort().toString().contains("publishedDateTime")
                ? "publishedDateTime"
                : "memberCount");
        return "search";

        // StudyRepository 에 전달받은 keyword 로 조회한 결과를 모델에 담아 search 페이지로 이동

        //기존 스터디 검색 API에 Pageable 파라미터를 추가하고 Study 를 List 타입이 아닌 Page 타입으로 변경한다.
        //@PageableDefault를 이용하여 페이지 사이즈와 페이지 , 정렬 방식 등의 기본 값을 지정할 수 있다.
        // 페이지 사이즈는 9로 정렬은 공개 날짜가 오래된 순으로 기본 값을 지정하였다.
    }
}
