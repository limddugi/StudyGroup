package io.lcalmsky.app.modules.study.domain.entity;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static javax.persistence.FetchType.EAGER;

@Entity
@NamedEntityGraph(name = "Study.withAll", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")
})
@NamedEntityGraph(name = "Study.withTagsAndManagers", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("managers"),
})
@NamedEntityGraph(name = "Study.withZonesAndManagers", attributeNodes = {
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
})
@NamedEntityGraph(name = "Study.withManagers", attributeNodes = {
        // 관리자 정보 fetch join 하도록 NamedEntityGraph를 추가하였다.
        @NamedAttributeNode("managers")
})
@NamedEntityGraph(name = "Study.withMembers", attributeNodes = {
        @NamedAttributeNode("members")
})
@NamedEntityGraph(name = "Study.withTagsAndZones", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones")

})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Study {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToMany
    private Set<Account> managers = new HashSet<>();
    // 관리자 변경 등을 고려해 관리자를 여러 명을 설정할 수 있게 한다.

    @ManyToMany
    private Set<Account> members = new HashSet<>();

    @Column(unique = true)
    private String path; // 스터디 페이지 경로이므로 유니크 해야한다.

    private String title;
    private String shortDescription;

    @Lob
    @Basic(fetch = EAGER)
    private String fullDescription; // 긴 설명을 255자를 넘어갈 수 있으니 @Lob을 사용,

    @Lob
    @Basic(fetch = EAGER)
    private String image; // 프로필 사진 때와 마찬가지로 @Lob으로 설정합니다.

    @ManyToMany
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany
    private Set<Zone> zones = new HashSet<>();

    private LocalDateTime localDateTime;
    private LocalDateTime closedDateTime;
    private LocalDateTime recruitingUpdatedDateTime;
    private LocalDateTime publishedDateTime;

    private boolean recruiting;
    private boolean published;
    private boolean closed;

    @Accessors(fluent = true)
    private boolean useBanner;

    @ColumnDefault(value = "0")
    private Integer memberCount = 0;

    public static Study from(StudyForm studyForm) {
        Study study = new Study();
        study.title = studyForm.getTitle();
        study.shortDescription = studyForm.getShortDescription();
        study.fullDescription = studyForm.getFullDescription();
        study.path = studyForm.getPath();
        return study;
    }

    public void addManager(Account account) {
        managers.add(account);
    }

    public boolean isJoinable(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        return this.isPublished() && this.isRecruiting() && !this.members.contains(account) && !this.managers.contains(account);
    }

    public boolean isMember(UserAccount userAccount) {
        return this.members.contains(userAccount.getAccount());
    }

    public boolean isManager(UserAccount userAccount) {
        return this.managers.contains(userAccount.getAccount());
    }

    public void updateDescription(StudyDescriptionForm studyDescriptionForm) {
        this.shortDescription = studyDescriptionForm.getShortDescription();
        this.fullDescription = studyDescriptionForm.getFullDescription();
    }

    public void updateImage(String image) {
        this.image = image;
    }

    public void setBanner(boolean useBanner) {
        this.useBanner = useBanner;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public void removeZone(Zone zone) {
        this.zones.remove(zone);
    }

    public void publish() {
        // 스터디의 이전 상태를 검사하여 예외를 던지거나 공개된 상태로 변경한다.
        if (this.closed || this.published) {
            throw new IllegalStateException("스터디를 이미 공개했거나 종료된 스터디 입니다.");
        }
        this.published = true;
        this.publishedDateTime = LocalDateTime.now();
    }

    public void close() {
        //스터디의 이전 상태를 검사하여 예외를 던지거나 종료된 상태로 변경한다.
        if (!this.published || this.closed) {
            throw new IllegalStateException("스터디를 공개하지 않았거나 이미 종료한 스터디 입니다.");
        }
        this.closed = true;
        this.closedDateTime = LocalDateTime.now();
    }

    public boolean isEnableToRecruit() {
        // 팀원 모집이 가능한 상태인지 검사한다.
        return this.published && this.recruitingUpdatedDateTime == null
                || this.recruitingUpdatedDateTime.isBefore(LocalDateTime.now().minusHours(1));
    }

    public void updatePath(String newPath) {
        // 스터디 경로를 변경한다.
        this.path = newPath;
    }

    public void updateTitle(String newTitle) {
        // 스터디 이름을 변경한다.
        this.title = newTitle;
    }

    public boolean isRemovable() {
        // 스터디를 제거할 수 있는 상태인지 확인
        return !this.published;
    }

    public void startRecruit() {
        // 스터디 상태를 검사하여 예외를 던지거나 팀원 모집 상태로 변경한다.
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 시작할 수 없습니다. 스터디를 공개하거나 한 시간뒤 다시 시도하세요");
        }
        this.recruiting = true;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }

    public void stopRecruit() {
        // // 스터디 상태를 검사하여 예외를 던지거나 팀원 모집 중단 상태로 변경한다.
        if (!isEnableToRecruit()) {
            throw new RuntimeException("인원 모집을 멈출 수 없습니다. 스터디를 공개하거나 한 시간 뒤 다시 시도하세요");
        }
        this.recruiting = false;
        this.recruitingUpdatedDateTime = LocalDateTime.now();
    }

    public void addMember(Account account) {
        this.members.add(account);
        this.memberCount++;
    }

    public void removeMember(Account account) {
        this.members.remove(account);
        this.memberCount--;
    }

    public String getEncodedPath() {
        return URLEncoder.encode(path, StandardCharsets.UTF_8);
    }

    public boolean isManagedBy(Account account) {
        return this.getManagers().contains(account);
    }
}
