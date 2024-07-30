package io.lcalmsky.app.modules.study.event;

import io.lcalmsky.app.infra.config.AppProperties;
import io.lcalmsky.app.infra.email.EmailMessage;
import io.lcalmsky.app.infra.email.EmailService;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.infra.predicates.AccountPredicates;
import io.lcalmsky.app.modules.account.infra.repository.AccountRepository;
import io.lcalmsky.app.modules.notification.domain.entity.Notification;
import io.lcalmsky.app.modules.notification.domain.entity.NotificationType;
import io.lcalmsky.app.modules.notification.repository.NotificationRepository;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.infra.repostiory.StudyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;


@Slf4j
@Async
@Transactional
@Component
@RequiredArgsConstructor
public class StudyEventListener {

    private final StudyRepository studyRepository;
    private final AccountRepository accountRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;

    @EventListener // 이 어노테이션을 이용해 이벤트 리스터를 명시
    public void handleStudyCreatedEvent(StudyCreatedEvent studyCreatedEvent) {
        //EventPublisher 를 통해 이벤트가 발생될 때 전달한 파라미터가 StudyCreatedEvent 일 때 해당 메서드가 호출된다.
        Study study = studyRepository.findStudyWithTagsAndZonesById(studyCreatedEvent.getStudy().getId());
        // 관심사와 지역 정보를 추가로 조회

        Iterable<Account> accounts = accountRepository.findAll(AccountPredicates.findByTagsAndZones(study.getTags(), study.getZones()));
        // 관심사와 지역정보에 해당하는 모든 계정을 찾는다.

        for (Account account : accounts) {
            // 계정을 순차적으로 탐색하면서 메일 알림 설정을 한 계정에는 메일을 전송하고, 웹 알림 설정을 한 계정은 웹 알림을 저장
            Account.NotificationSetting notificationSetting = account.getNotificationSetting();
            if (notificationSetting.isStudyCreatedByEmail()) {
                sendEmail(study, account, "새로운 스터디가 오픈하였습니다.", "[FRITZ]" +
                        study.getTitle() + "스터디가 오픈하였습니다.");
            }
            if (notificationSetting.isStudyCreatedByWeb()) {
                saveNotification(study, account, NotificationType.STUDY_CREATED, study.getShortDescription());
            }
        }
    }

    @EventListener
    public void handleStudyUpdateEvent(StudyUpdateEvent studyUpdateEvent) {
        Study study = studyRepository.findStudyWithManagersAndMembersById(studyUpdateEvent.getStudy().getId());
        Set<Account> accounts = new HashSet<>();
        accounts.addAll(study.getManagers());
        accounts.addAll(study.getMembers());
        accounts.forEach(account -> {
            if (account.getNotificationSetting().isStudyUpdatedByEmail()) {
                sendEmail(study, account, studyUpdateEvent.getMessage(), "[FRITZ]" + study.getTitle() + " 스터디에 새소식이 있습니다.");
            }
            if (account.getNotificationSetting().isStudyUpdatedByWeb()) {
                saveNotification(study, account, NotificationType.STUDY_UPDATED, studyUpdateEvent.getMessage());
            }
        });
    }

    private void sendEmail(Study study, Account account, String contextMessage, String emailSubject) {
        Context context = new Context();
        context.setVariable("link", "/study/" + study.getEncodedPath());
        context.setVariable("nickname", account.getNickname());
        context.setVariable("linkName", study.getTitle());
        context.setVariable("message", contextMessage);
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        emailService.sendEmail(EmailMessage.builder()
                .to(account.getEmail())
                .subject(emailSubject)
                .message(message)
                .build());
    }

    private void saveNotification(Study study, Account account, NotificationType notificationType, String message) {
        notificationRepository.save(Notification.from(study.getTitle(), "/study/" + study.getEncodedPath(),
                false, LocalDateTime.now(), message, account, notificationType));
    }
}
