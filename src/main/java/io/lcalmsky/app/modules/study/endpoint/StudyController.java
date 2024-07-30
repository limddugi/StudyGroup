package io.lcalmsky.app.modules.study.endpoint;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.study.application.StudyService;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.study.endpoint.form.validator.StudyFormValidator;
import io.lcalmsky.app.modules.study.infra.repostiory.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class StudyController {

    private final StudyService studyService;
    private final StudyFormValidator studyFormValidator;
    private final StudyRepository studyRepository;
    // 스터디 정보를 다둘 StudyService와 스터디 폼을 검증한 StudyFormValidator를 주입받는다.

    @InitBinder("studyForm")
    // studyForm을 검증한 StudyFormValidator를 WebDataBinder를 이용해 추가
    public void studyFormInitBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(studyFormValidator);
    }

    @GetMapping("/new-study")
    // 스터디 개설 버턴을 눌렀을 때 스터디 생성 페이지로 이동시켜준다. 현재 계정 정보와 StudyForm을 넘겨준다.
    public String newStudyForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new StudyForm());
        return "study/form";
    }

    @PostMapping("/new-study")
    // 스터디 생성 버튼을 눌렀을 때 에러가 있을 경우 에러를 전달하고, 에러가 없을 경우 스터디 생성 후 생성된 스터디 페이지로 이동
    public String newStudySubmit(@CurrentUser Account account, @Valid StudyForm studyForm, Errors errors) {
        if (errors.hasErrors()) {
            return "study/form";
        }
        Study newStudy = studyService.createNewStudy(studyForm, account);
        return "redirect:/study/" + URLEncoder.encode(newStudy.getPath(), StandardCharsets.UTF_8);
    }

    @GetMapping("/study/{path}")
    public String viewStudy(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyService.getStudy(path));
        return "study/view";
    }

    @GetMapping("/study/{path}/members")
    public String viewStudyMembers(@CurrentUser Account account, @PathVariable String path, Model model) {
        model.addAttribute(account);
        model.addAttribute(studyService.getStudy(path));
        return "study/members";
    }

    @GetMapping("/study/{path}/join")
    public String joinStudy(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyRepository.findStudyWithMembersByPath(path);
        studyService.addMember(study, account);
        return "redirect:/study/" + study.getEncodedPath() + "/members";
    }

    @GetMapping("/study/{path}/leave")
    public String leaveStudy(@CurrentUser Account account, @PathVariable String path) {
        Study study = studyRepository.findStudyWithManagersByPath(path);
        studyService.removeMember(study, account);
        return "redirect:/study/" + study.getEncodedPath() + "/members";
    }
}
