package io.lcalmsky.app.modules.account.endpoint.controller.form;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import lombok.Data;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

@Data
@NoArgsConstructor
public class NotificationForm {

    private boolean studyCreatedByEmail;
    private boolean studyCreatedByWeb;
    private boolean studyRegistrationResultByEmail;
    private boolean studyRegistrationResultByWeb;
    private boolean studyUpdatedByEmail;
    private boolean studyUpdatedByWeb;

    protected NotificationForm(Account account) {
        this.studyCreatedByEmail = account.getNotificationSetting().isStudyCreatedByEmail();
        this.studyCreatedByWeb = account.getNotificationSetting().isStudyCreatedByWeb();
        this.studyUpdatedByEmail = account.getNotificationSetting().isStudyUpdatedByEmail();
        this.studyUpdatedByWeb = account.getNotificationSetting().isStudyUpdatedByWeb();
        this.studyRegistrationResultByEmail = account.getNotificationSetting().isStudyRegistrationResultByEmail();
        this.studyRegistrationResultByWeb = account.getNotificationSetting().isStudyRegistrationResultByWeb();
    }

    public static NotificationForm from(Account account) {
        return new NotificationForm(account);
    }
}
