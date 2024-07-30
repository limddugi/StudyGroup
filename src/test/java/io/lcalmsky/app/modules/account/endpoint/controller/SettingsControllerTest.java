package io.lcalmsky.app.modules.account.endpoint.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lcalmsky.app.modules.account.WithAccount;
import io.lcalmsky.app.modules.account.application.AccountService;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.endpoint.controller.form.TagForm;
import io.lcalmsky.app.modules.account.endpoint.controller.form.ZoneForm;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.zone.repository.ZoneRepository;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import io.lcalmsky.app.modules.tag.infra.repository.TagRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static io.lcalmsky.app.modules.account.endpoint.controller.SettingsController.SETTINGS_TAGS_URL;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
// 태그 추가, 삭제 테스트시 @Transactional이 존재하지 않으면 Entity의 상태 관리가 제대로 되지 않는다.
// Service 레이어에 @Transactional이 존재하기 떄문에 Service 레이어를 벗어난 이후에는 Entity의 상태가 detachedrk 되기 때문이다.
// 뷰가 없이 추가, 삭제 여부를 확인하기 위해선 DB조회를 추가로 해야 하므로 @Transactional 어노테이션을 꼭 추가해야한다.
public class SettingsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired PasswordEncoder passwordEncoder; // 비밀번호 검증을 위해 주입
    @Autowired AccountService accountService;
    @Autowired TagRepository tagRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired ZoneRepository zoneRepository;


    @AfterEach
    void afterEach() { // WithAccount 어노테이션을 통해 인증 정보를 주입할 때 DB에 해당 정보가
        // 저장되므로 테스트가 끝나면 반드시 삭제해줘야 다른 테스트에 영향을 미치지 않는다.
        accountRepository.deleteAll();
        zoneRepository.deleteAll();
    }

    @DisplayName("프로필 수정 : 입력값 정상")
    @Test
    @WithAccount("jaime")
    void updateProfile() throws Exception {
        String bio = "한 줄 소개";
        mockMvc.perform(post(SettingsController.SETTINGS_PROFILE_URL)
                //SettingsController 의 상수 접근 레벨을 수정한 이유. URL을 상수로 전달하면 오타를 방지
                        .param("bio", bio)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SettingsController.SETTINGS_PROFILE_URL))
                // 정상 처리 되었을 경우 다시 동일한 페이지로 리다이렉트한다.
                .andExpect(flash().attributeExists("message"));
                // 정상일 경우 flashAttribute로 메시지를 전달하여 수정 완료되었다는 UI 피드백을 전달하므로
                // 해당 키가 존재하는지 확인

        Account jaime = accountRepository.findByNickname("jaime");
        // DB에 저장된 사용자 정보를 불러와 profile 이 정확하게 업데이트 되었는지 확인
        assertEquals(bio, jaime.getProfile().getBio());
    }

    @DisplayName("프로필 수정 : 입력값 에러")
    @Test
    @WithAccount("jaime")
    void updateProfileWithError() throws Exception {
        String bio = "35자 넘으면 에러35자 넘으면 에러35자 넘으면 에러35자 넘으면 에러";
        //Profile 클래스에 추가한 validation을 테스트하기 위해 한 줄 소개를 35자보다 길게 설정

        mockMvc.perform(post(SettingsController.SETTINGS_PROFILE_URL)
                        .param("bio", bio)
                        .with(csrf()))
                .andExpect(status().isOk()) // 응답은 200 OK 지만 에러를 전달한다.
                .andExpect(view().name(SettingsController.SETTINGS_PROFILE_VIEW_NAME))
                // 리다이렉트 되는 것이 아니라 해당 뷰를 다시 보여준다
                .andExpect(model().hasErrors()) // 에러 객체가 있는지 확인
                .andExpect(model().attributeExists("account"))
                // SettingsController 에서 에러일 경우 account 와 profile 객체를 전달하도록 작성하였는데 제대로 동작하는지 확인
                .andExpect(model().attributeExists("profile"));


        Account jaime = accountRepository.findByNickname("jaime");

        assertNull(jaime.getProfile().getBio()); // DB에 소개가 업데이트 되지 않았을 것이므로 null이어야 한다.
    }

    @DisplayName("프로필 조회")
    @Test
    @WithAccount("jaime")
    void updateProfileForm() throws Exception {
        String bio = "한 줄 소개";
        mockMvc.perform(get(SettingsController.SETTINGS_PROFILE_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PROFILE_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"));

    }

    @Test
    @DisplayName("패스워드 수정 폼")
    @WithAccount("jaime") // 이전 포스팅의 SecurityContext 설정 항목을 참조
    void updatePasswordForm() throws Exception { // 패스워드 수정 뷰에 진입했을 때 정확하게 동작하는지 확인
        mockMvc.perform(get(SettingsController.SETTINGS_PASSWORD_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PASSWORD_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
    }

    @Test
    @DisplayName("패스워드 수정: 입력값 정상")
    @WithAccount("jaime")
    void updatePassword() throws Exception {
        // 입력값이 정상일 때 정상적으로 리다이렉트되는지, flashAttribute 메시지 피드백이 전달 되는지, 비밀번호 저장이 정확하게 동작했는지 확인

        mockMvc.perform(post(SettingsController.SETTINGS_PASSWORD_URL)
                        .param("newPassword", "12341234")
                        .param("newPasswordConfirm", "12341234")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SettingsController.SETTINGS_PASSWORD_URL))
                .andExpect(flash().attributeExists("message"));
        Account account = accountRepository.findByNickname("jaime");
        assertTrue(passwordEncoder.matches("12341234", account.getPassword()));
    }

    @Test
    @DisplayName("패스워드 수정: 입력값 에러(불일치)")
    @WithAccount("jaime")
    void updatePasswordWithNotMatchedError() throws Exception {
        // 비밀번호 불일치시 200 OK 응답 후 다시 패스워드 뷰를 보여주면서 에러가 전달 되는지 확인

        mockMvc.perform(post(SettingsController.SETTINGS_PASSWORD_URL)
                        .param("newPassword", "12341234")
                        .param("newPasswordConfirm", "12121212")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PASSWORD_VIEW_NAME))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("passwordForm"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @DisplayName("패스워드 수정: 입력값 에러(길이)")
    @WithAccount("jaime")
    void updatePasswordWithLengthError() throws Exception {
        // 비밀번호 길이가 유효하지 않을 때 200 OK 응답 후 다시 패스워드 뷰를 보여주면서 에러가 전달되는지 확인

        mockMvc.perform(post(SettingsController.SETTINGS_PASSWORD_URL)
                        .param("newPassword", "1234")
                        .param("newPasswordConfirm", "1234")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_PASSWORD_VIEW_NAME))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("passwordForm"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @DisplayName("알림 설정 수정 폼")
    @WithAccount("jaime")
    void updateNotificationForm() throws Exception {
        mockMvc.perform(get(SettingsController.SETTINGS_NOTIFICATION_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_NOTIFICATION_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("notificationForm"));
    }
    @Test
    @DisplayName("알림 설정 수정: 입력값 정상")
    @WithAccount("jaime")
    void updateNotification() throws Exception {
        mockMvc.perform(post(SettingsController.SETTINGS_NOTIFICATION_URL)
                        .param("studyCreatedByEmail", "true")
                        .param("studyCreatedByWeb", "true")
                        .param("studyRegistrationResultByEmail", "true")
                        .param("studyRegistrationResultByWeb", "true")
                        .param("studyUpdatedByEmail", "true")
                        .param("studyUpdatedByWeb", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SettingsController.SETTINGS_NOTIFICATION_URL))
                .andExpect(flash().attributeExists("message"));
        Account account = accountRepository.findByNickname("jaime");
        assertTrue(account.getNotificationSetting().isStudyCreatedByEmail());
        assertTrue(account.getNotificationSetting().isStudyCreatedByWeb());
        assertTrue(account.getNotificationSetting().isStudyRegistrationResultByEmail());
        assertTrue(account.getNotificationSetting().isStudyRegistrationResultByWeb());
        assertTrue(account.getNotificationSetting().isStudyUpdatedByEmail());
        assertTrue(account.getNotificationSetting().isStudyUpdatedByWeb());
    }

    @DisplayName("닉네임 수정 폼")
    @Test
    @WithAccount("jaime")
    void updateNicknameForm() throws Exception {
        mockMvc.perform(get(SettingsController.SETTINGS_ACCOUNT_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_ACCOUNT_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("nicknameForm"));
    }

    @DisplayName("닉네임 수정 : 입력값 정상")
    @Test
    @WithAccount("jaime")
    void updateNickname() throws Exception {
        String newNickname = "jaime2";
        mockMvc.perform(post(SettingsController.SETTINGS_ACCOUNT_URL)
                        .param("nickname", newNickname)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SettingsController.SETTINGS_ACCOUNT_URL))
                .andExpect(flash().attributeExists("message"));
        Account account = accountRepository.findByNickname(newNickname);
        assertEquals(newNickname, account.getNickname());
    }

    @DisplayName("닉네임 수정 : 에러(길이)")
    @Test
    @WithAccount("jaime")
    void updateNicknameWithShortNickname() throws Exception {
        String newNickname = "j";
        mockMvc.perform(post(SettingsController.SETTINGS_ACCOUNT_URL)
                        .param("nickname", newNickname)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_ACCOUNT_VIEW_NAME))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("nicknameForm"))
                .andExpect(model().attributeExists("account"));
    }

    @DisplayName("닉네임 수정: 에러(중복)")
    @Test
    @WithAccount("jaime")
    void updateNicknameWithDuplicatedNickname() throws Exception {
        String newNickname = "jaime";
        mockMvc.perform(post(SettingsController.SETTINGS_ACCOUNT_URL)
                        .param("nickname", newNickname)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_ACCOUNT_VIEW_NAME))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("nicknameForm"))
                .andExpect(model().attributeExists("account"));
    }

    @Test
    @DisplayName("태그 수정 폼")
    @WithAccount("jaime")
    void updateTagForm() throws Exception {
        // (3)
        mockMvc.perform(get(SETTINGS_TAGS_URL))
                .andExpect(status().isOk())
                .andExpect(view().name(SettingsController.SETTINGS_TAGS_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("whitelist"))
                .andExpect(model().attributeExists("tags"));
    }

    @DisplayName("태그 추가")
    @Test
    @WithAccount("jaime")
    void addTag() throws Exception {
        // 태그 추가시 전달할 TagForm 객체를 생성해 값을 할당한다.
        TagForm tagForm = new TagForm();
        String tagTitle = "newTag";
        tagForm.setTagTitle(tagTitle);

        mockMvc.perform(post(SETTINGS_TAGS_URL + "/add")
                //body로 tagForm 객체를 전달하는데 JSON 형태로 변환하여 전달한다.
                //post 요청시에는 반드시 csrf 토큰이 필요하다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagForm))
                        .with(csrf()))
                .andExpect(status().isOk());

        Tag tag = tagRepository.findByTitle(tagTitle).orElse(null);
        // 태그가 잘 추가되었는지 확인
        assertNotNull(tag);
        assertTrue(accountRepository.findByNickname("jaime").getTags().contains(tag));
        //계정에 태그가 추가되었는지 확인한다. @Transactional이 없으면 이 곳에서 에러가 발생한다.
        //그 이유는 Account를 찾은 뒤 getTags를 호출할 때 추가 transaction이 발생해야하는데
        // 이미 EntityManager에서 관리하지 않는 detached 상태가 되기 때문이다.
    }

    @DisplayName("태그 삭제")
    @Test
    @WithAccount("jaime")
    void removeTag() throws Exception{
        // 태그 추가와 비슷하게 테스트 코드를 작성한다. 먼저 추개해놓은 상태에서 삭제를 수행해야 한다.
        Account jaime = accountRepository.findByNickname("jaime");
        Tag newTag = tagRepository.save(Tag.builder().title("newTag").build());

        accountService.addTag(jaime, newTag);
        //accountService의 메서드를 이용한다.
        assertTrue(jaime.getTags().contains(newTag));
        //태그가 정확히 추가되었는지 확인

        // 태그 폼 객체 생성 후 값을 할당
        TagForm tagForm = new TagForm();
        String tagTitle = "newTag";
        tagForm.setTagTitle(tagTitle);
        mockMvc.perform(post(SETTINGS_TAGS_URL + "/remove")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tagForm))
                        .with(csrf()))
                .andExpect(status().isOk());

        // 요청 수행 후 계정에서 태그가 존재하지 않아야 한다.
        assertFalse(jaime.getTags().contains(newTag));
    }

    @DisplayName("계정의 지역 정보 수정 폼")
    @Test
    @WithAccount("jaime")
    void updateZonesForm() throws Exception {
        mockMvc.perform(get(SettingsController.SETTINGS_ZONE_URL))
                .andExpect(view().name(SettingsController.SETTINGS_ZONE_VIEW_NAME))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("whitelist"))
                .andExpect(model().attributeExists("zones"));
    }

    @DisplayName("계정의 지역 정보 추가")
    @Test
    @WithAccount("jaime")
    void addZone() throws Exception {
        Zone testZone = Zone.builder().city("test").localNameOfCity("테스트시").province("테스트주").build();
        zoneRepository.save(testZone);
        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());

        mockMvc.perform(post(SettingsController.SETTINGS_ZONE_URL + "/add")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneForm))
                        .with(csrf()))
                .andExpect(status().isOk());
        Account account = accountRepository.findByNickname("jaime");
        assertTrue(account.getZones().contains(testZone));
    }

    @DisplayName("계정의 지역 정보 삭제")
    @Test
    @WithAccount("jaime")
    void removeZone() throws Exception {
        Account account = accountRepository.findByNickname("jaime");
        Zone testZone = Zone.builder().city("test").localNameOfCity("테스트시").province("테스트주").build();

        zoneRepository.save(testZone);
        accountService.addZone(account, testZone);

        assertTrue(account.getZones().contains(testZone));

        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());
        mockMvc.perform(post(SettingsController.SETTINGS_ZONE_URL + "/remove")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneForm))
                        .with(csrf()))
                .andExpect(status().isOk());
        assertFalse(account.getZones().contains(testZone));
    }
}



