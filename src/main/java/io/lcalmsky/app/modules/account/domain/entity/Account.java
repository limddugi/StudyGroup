package io.lcalmsky.app.modules.account.domain.entity;

import io.lcalmsky.app.modules.account.endpoint.controller.form.NotificationForm;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.*;
import org.hibernate.Hibernate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static javax.persistence.FetchType.EAGER;
import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @Getter @ToString
public class Account extends AuditingEntity {

    @Id @GeneratedValue
    @Column(name = "account_id")
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    private boolean isValid;

    private String emailToken;

    private LocalDateTime joinedAt;

    @Embedded
    private Profile profile = new Profile();

    @Embedded
    private NotificationSetting notificationSetting = new NotificationSetting();

    private LocalDateTime emailTokenGeneratedAt; // 이메일 토큰이 발급한 시기를 저장할 수 있는 필드변수를 생성

    @ManyToMany @ToString.Exclude
    private Set<Tag> tags = new HashSet<>();
    // 컬렉션 타입의 경우 비어있는 객체로 조기화해준다. @ToString이 있을 경우 순환참조하여 에러가 발생하기 떄문에 Exclude를 추가해준다.

    @ManyToMany @ToString.Exclude
    private Set<Zone> zones = new HashSet<>();

    public static Account with(String email, String nickname, String password) {
        //static 생성자로 이메일 , 닉네임, 비밀번호를 초기화
        Account account = new Account();
        account.email = email;
        account.nickname = nickname;
        account.password = password;
        return account;
    }


    public void generateToken() {
        this.emailToken = UUID.randomUUID().toString();
        this.emailTokenGeneratedAt = LocalDateTime.now(); //토큰을 발급할 때 발급 시기를 업데이트
    }

    public boolean enableToSendEmail() { //
        return this.emailTokenGeneratedAt.isBefore(LocalDateTime.now().minusMinutes(5));
        // 이메일을 보낼 수 있는지 체크한다. 5분이 지났는지 체크
    }

    public void verified() {
        this.isValid = true;
        joinedAt = LocalDateTime.now();
    }

    @PostLoad
    private void init() {
        if (profile == null) {
            profile = new Profile();
        }
        if (notificationSetting == null) {
            notificationSetting = new NotificationSetting();
        }

    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;

    }

    public void updateNotification(NotificationForm notificationForm) {
        this.notificationSetting.studyCreatedByEmail = notificationForm.isStudyCreatedByEmail();
        this.notificationSetting.studyCreatedByWeb = notificationForm.isStudyCreatedByWeb();
        this.notificationSetting.studyUpdatedByWeb = notificationForm.isStudyUpdatedByWeb();
        this.notificationSetting.studyUpdatedByEmail = notificationForm.isStudyUpdatedByEmail();
        this.notificationSetting.studyRegistrationResultByEmail = notificationForm.isStudyRegistrationResultByEmail();
        this.notificationSetting.studyRegistrationResultByWeb = notificationForm.isStudyRegistrationResultByWeb();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isValid(String token) {
        return this.emailToken.equals(token);
    }



    @Embeddable
    @NoArgsConstructor(access = PROTECTED)
    @AllArgsConstructor(access = PROTECTED)
    @Getter @Builder @ToString
    public static class Profile {
        private String bio;
        private String url;
        private String job;
        private String location;
        private String company;

        @Lob @Basic(fetch = EAGER)
        // 데이터베이스 BLOB, CLOB 타입과 매핑
        //• @Lob에는 지정할 수 있는 속성이 없다.
        //• 매핑하는 필드 타입이 문자면 CLOB 매핑, 나머지는 BLOB 매핑
        // • CLOB: String, char[], java.sql.CLOB
        //• BLOB: byte[], java.sql. BLOB
        // @Lob의 경우 String 은 CLOB으로 생성된다.
        private String image;
    }

    public void updateProfile(io.lcalmsky.app.modules.account.endpoint.controller.form.Profile profile) {
        if (this.profile == null) {
            this.profile = new Profile();
        }
        this.profile.bio = profile.getBio();
        this.profile.url = profile.getUrl();
        this.profile.job = profile.getJob();
        this.profile.location = profile.getLocation();
        this.profile.image = profile.getImage(); // 업데이트 시 이미지 필드에 값을 할당해준다.
    }

    @Embeddable
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @Getter
    @ToString
    public static class NotificationSetting {
        private boolean studyCreatedByEmail = false;
        private boolean studyCreatedByWeb = true;
        private boolean studyRegistrationResultByEmail = false;
        private boolean studyRegistrationResultByWeb = true;
        private boolean studyUpdatedByEmail = false;
        private boolean studyUpdatedByWeb = true;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Account account = (Account) o;
        return id != null && Objects.equals(id, account.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }


}
