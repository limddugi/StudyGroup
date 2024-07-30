package io.lcalmsky.app.modules.account.endpoint.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.modules.account.application.AccountService;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.form.*;
import io.lcalmsky.app.modules.account.endpoint.controller.validator.NicknameFormValidator;
import io.lcalmsky.app.modules.account.endpoint.controller.validator.PasswordFormValidator;
import io.lcalmsky.app.modules.zone.repository.ZoneRepository;
import io.lcalmsky.app.modules.account.support.CurrentUser;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import io.lcalmsky.app.modules.tag.infra.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    public static final String SETTINGS_PROFILE_VIEW_NAME = "settings/profile"; // 자주 사용되는 값들을 상수로 정의
    public static final String SETTINGS_PROFILE_URL = "/" + SETTINGS_PROFILE_VIEW_NAME; // 자주 사용되는 값들을 상수로 정의
    static final String SETTINGS_PASSWORD_VIEW_NAME = "settings/password"; // password url 과 view를 상수로 지정
    static final String SETTINGS_PASSWORD_URL = "/" + SETTINGS_PASSWORD_VIEW_NAME; // password url 과 view를 상수로 지정
    static final String SETTINGS_NOTIFICATION_VIEW_NAME = "settings/notification"; // NOTIFICATION_URL 과 view를 상수로 지정
    static final String SETTINGS_NOTIFICATION_URL = "/" + SETTINGS_NOTIFICATION_VIEW_NAME;
    static final String SETTINGS_ACCOUNT_VIEW_NAME = "settings/account";
    static final String SETTINGS_ACCOUNT_URL = "/" + SETTINGS_ACCOUNT_VIEW_NAME;
    static final String SETTINGS_TAGS_VIEW_NAME = "settings/tags";
    static final String SETTINGS_TAGS_URL = "/" + SETTINGS_TAGS_VIEW_NAME;
    static final String SETTINGS_ZONE_VIEW_NAME = "settings/zones";
    static final String SETTINGS_ZONE_URL = "/" + SETTINGS_ZONE_VIEW_NAME;


    private final AccountService accountService; // 프로필 업데이트를 위임함에 사용
    private final PasswordFormValidator passwordFormValidator;
    private final NicknameFormValidator nicknameFormValidator;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;
    private final ZoneRepository zoneRepository;

    @InitBinder("passwordForm")
    public void passwordFormValidator(WebDataBinder webDataBinder) {
        // 패스워드 폼을 검증하기 위한 validator를 추가한다. 예전에 회원 가입 폼을 검증할 때 사용했던 방법과 동일
        webDataBinder.addValidators(passwordFormValidator);
    }

    @InitBinder("nicknameForm")
    public void nicknameFormInitBinder(WebDataBinder webDataBinder) {
        webDataBinder.addValidators(nicknameFormValidator);
    }

    @GetMapping(SETTINGS_PROFILE_URL) // 기존 문자열을 상수 변수로 대체한다
    public String profileUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(Profile.from(account));
        return SETTINGS_PROFILE_VIEW_NAME;
    }

    @PostMapping(SETTINGS_PROFILE_URL) // 기존 문자열을 상수 변수로 대체한다
    public String updateProfile(@CurrentUser Account account, @Valid Profile profile, Errors errors, Model model, RedirectAttributes attributes) {
        // 현재 사용자의 계정 정보와 프로필 폼을 통해 전달된 정보를 받는다. Profile 폼을 validation 할 때 발생하는 에러들은
        // Errors 객체를 통해 전달받는다. 다시 뷰로 데이터를 전달하기 위한 model 객체도 주입받는다.
        // RedirectAttributes는 리다이렉트 시 1회성 데이터를 전달할 수 있는 객체이므로 컨트롤러로 주입해줍니다.
        if (errors.hasErrors()) {
            //에러가 있으면 model에 폼을 채웠던 데이터와 에러 관련된 데이터는 자동으로 전달되므로 계정정보만
            // 추가로 전달하고 다시 해당 뷰를 보여준다.
            model.addAttribute(account);
            return SETTINGS_PROFILE_VIEW_NAME;
        }
        accountService.updateProfile(account, profile); //프로필 업데이트를 위임함에 사용
        attributes.addFlashAttribute("message", "프로필을 수정하였습니다.");
        // 리다이렉트 시 addFlashAttribute를 이용해 1회성 데이터를 전달합니다.
        // 앞서 에러인 경우에 대해 처리했기 때문에 성공했을 때 전달할 메시지를 attribute로 추가합니다.

        return "redirect:" + SETTINGS_PROFILE_URL;
        // 사용자가 화면을 refresh 하더라도 form submit이 다시 발생하지 않도록 redirect 한다
    }

    @GetMapping(SETTINGS_PASSWORD_URL) // 패스워드 수정 뷰로 라우팅해준다. 현재 계정 정보를 Model로 넘겨준다.
    public String passUpdateForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new PasswordForm());
        return SETTINGS_PASSWORD_VIEW_NAME;
    }

    @PostMapping(SETTINGS_PASSWORD_URL) // 패스워드 폼을 전달받아 해당 패스워드로 업데이트 한다.
    // 에러가 있을 경우 다시 페이즈를 띄우고 그렇지 않을 경우 피드백 메시지와 함께 리다이렉트 한다.
    public String updatePassword(@CurrentUser Account account, @Valid PasswordForm passwordForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_PASSWORD_VIEW_NAME;
        }
        accountService.updatePassword(account, passwordForm.getNewPassword()); // 비밀번호 변경은 Service에게 위임
        attributes.addFlashAttribute("message", "패스워드를 변경했습니다.");
        return "redirect:" + SETTINGS_PASSWORD_URL;
    }

    @GetMapping(SETTINGS_NOTIFICATION_URL)
    public String notificationForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(NotificationForm.from(account));
        return SETTINGS_NOTIFICATION_VIEW_NAME;
    }

    @PostMapping(SETTINGS_NOTIFICATION_URL)
    public String updateNotification(@CurrentUser Account account, @Valid NotificationForm notificationForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_NOTIFICATION_URL;
        }
        accountService.updateNotification(account, notificationForm);
        attributes.addFlashAttribute("message", "알림설정을 수정하였습니다.");
        return "redirect:" + SETTINGS_NOTIFICATION_URL;
    }

    @GetMapping(SETTINGS_ACCOUNT_URL)
    public String nicknameForm(@CurrentUser Account account, Model model) {
        model.addAttribute(account);
        model.addAttribute(new NicknameForm(account.getNickname()));
        return SETTINGS_ACCOUNT_VIEW_NAME;
    }

    @PostMapping(SETTINGS_ACCOUNT_URL)
    public String updateNickname(@CurrentUser Account account, @Valid NicknameForm nicknameForm, Errors errors, Model model, RedirectAttributes attributes) {
        if (errors.hasErrors()) {
            model.addAttribute(account);
            return SETTINGS_ACCOUNT_VIEW_NAME;
        }
        accountService.updateNickname(account, nicknameForm.getNickname());
        attributes.addFlashAttribute("message", "닉네임을 수정하였습니다.");
        return "redirect:" + SETTINGS_ACCOUNT_URL;
    }

    @GetMapping(SETTINGS_TAGS_URL)
    public String updateTags(@CurrentUser Account account, Model model) {
        model.addAttribute(account);

        Set<Tag> tags = accountService.getTags(account);
        if (tags == null) {
            tags = new HashSet<>();
        }

        model.addAttribute("tags", tags.stream()
                .map(Tag::getTitle)
                .collect(toList()));
        List<String> allTags = tagRepository.findAll() //TagRepository 에서 전체 태그 목록을 가져와 리스트로 반환
                .stream()
                .map(Tag::getTitle)
                .collect(toList());

        String whitelist = null;
        try {
            whitelist = objectMapper.writeValueAsString(allTags); // List를 JSON 형태로 변환
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        model.addAttribute("whitelist", whitelist); // whitelist를 model로 전달
        return SETTINGS_TAGS_VIEW_NAME;
    }

    @PostMapping(SETTINGS_TAGS_URL + "/add")
    @ResponseStatus(HttpStatus.OK)
    public void addTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        Tag tag = tagRepository.findByTitle(title)
                .orElseGet(() -> tagRepository.save(Tag.builder()
                        .title(title)
                        .build()));
        accountService.addTag(account, tag);
    }

    @PostMapping(SETTINGS_TAGS_URL + "/remove")
    @ResponseStatus(HttpStatus.OK)
    public void removeTag(@CurrentUser Account account, @RequestBody TagForm tagForm) {
        String title = tagForm.getTagTitle();
        Tag tag = tagRepository.findByTitle(title)
                .orElseThrow(IllegalArgumentException::new);
        accountService.removeTag(account, tag);
    }

    @GetMapping(SETTINGS_ZONE_URL)
    public String updateZonesForm(@CurrentUser Account account, Model model) throws JsonProcessingException {
        model.addAttribute(account);
        Set<Zone> zones = accountService.getZones(account);
        model.addAttribute("zones", zones.stream()
                .map(Zone::toString)
                .collect(toList()));
        List<String> allZones = zoneRepository.findAll().stream()
                .map(Zone::toString)
                .collect(toList());
        model.addAttribute("whitelist", objectMapper.writeValueAsString(allZones));
        return SETTINGS_ZONE_VIEW_NAME;
    }

    @PostMapping(SETTINGS_ZONE_URL + "/add")
    @ResponseStatus(HttpStatus.OK)
    public void addZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvinceAndLocalNameOfCity(zoneForm.getCityName(), zoneForm.getProvinceName(), zoneForm.getLocalNameOfCity())
                .orElseThrow(IllegalArgumentException::new);
        accountService.addZone(account, zone);
    }

    @PostMapping(SETTINGS_ZONE_URL + "/remove")
    @ResponseStatus(HttpStatus.OK)
    public void removeZone(@CurrentUser Account account, @RequestBody ZoneForm zoneForm) {
        Zone zone = zoneRepository.findByCityAndProvinceAndLocalNameOfCity(zoneForm.getCityName(), zoneForm.getProvinceName(), zoneForm.getLocalNameOfCity())
                .orElseThrow(IllegalArgumentException::new);
        accountService.removeZone(account, zone);
    }
}