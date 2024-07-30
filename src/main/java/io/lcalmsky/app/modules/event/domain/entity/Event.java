package io.lcalmsky.app.modules.event.domain.entity;

import io.lcalmsky.app.modules.account.domain.UserAccount;
import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.event.endpoint.form.EventForm;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import lombok.*;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PROTECTED;


@NamedEntityGraph(
        name = "Event.withEnrollments",
        attributeNodes = @NamedAttributeNode("enrollments")
)
@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
@ToString
public class Event {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Study study;

    @ManyToOne
    private Account createdBy;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdDateTime;

    @Column(nullable = false)
    private LocalDateTime endEnrollmentDateTime;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    private Integer limitOfEnrollments;


    @Enumerated(EnumType.STRING)
    private EventType eventType;
    public static Event from(EventForm eventForm, Account account, Study study) {
        Event event = new Event();
        event.eventType = eventForm.getEventType();
        event.description = eventForm.getDescription();
        event.endDateTime = eventForm.getEndDateTime();
        event.endEnrollmentDateTime = eventForm.getEndEnrollmentDateTime();
        event.limitOfEnrollments = eventForm.getLimitOfEnrollments();
        event.startDateTime = eventForm.getStartDateTime();
        event.title = eventForm.getTitle();
        event.createdBy = account;
        event.study = study;
        event.createdDateTime = LocalDateTime.now();
        return event;
    }

    private boolean isNotClosed() {
        return this.endEnrollmentDateTime.isAfter(LocalDateTime.now());
    }

    @OneToMany(mappedBy = "event") @ToString.Exclude
    @OrderBy("enrolledAt")
    // 참석 리스트가 조회될 떄 참석날짜 기준으로 정렬한다. 이렇게 하지 않으면 참가 상태가 바뀔 때마다 리스트의 순서가 랜덤으로 바뀔 수 있다.
    private List<Enrollment> enrollments = new ArrayList<>();

    public boolean isEnrollableFor(UserAccount userAccount) {
        // 참석하지 않은 상태인지 확인하는 부분을 추가하였다.
        return isNotClosed() && !isAttended(userAccount) && !isAlreadyEnrolled(userAccount);
    }

    public boolean isDisenrollableFor(UserAccount userAccount) {
        // 참석하지 않은 상태인지 확인하는 부분을 추가하였다.
        return isNotClosed() && isAlreadyEnrolled(userAccount);
    }

    public void accept(Enrollment enrollment) {
        // 관리자 확인 모임이고 참석 가능할 경우 참가의 상태를 변경
        if (this.eventType == EventType.CONFIRMATIVE && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments()) {
            enrollment.accept();
        }
    }

    public void reject(Enrollment enrollment) {
        // 관리자 확인 모임인 경우 참가의 상태를 변경
        if (this.eventType == EventType.CONFIRMATIVE) {
            enrollment.reject();
        }
    }

    public boolean isAcceptable(Enrollment enrollment) {
        // 수락 가능 상태인지 확인
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments()
                && !enrollment.isAttended()
                && !enrollment.isAccepted();
    }

    public boolean isRejectable(Enrollment enrollment) {
        // 거절 가능 상태인지 확인
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && !enrollment.isAttended()
                && enrollment.isAccepted();
    }


    private boolean isAlreadyEnrolled(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        for (Enrollment enrollment : this.enrollments) {
            if (enrollment.getAccount().equals(account)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAttended(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        for (Enrollment enrollment : this.enrollments) {
            if (enrollment.getAccount().equals(account) && enrollment.isAttended()) {
                return true;
            }
        }
        return false;
    }

    public int numberOfRemainSpots() {
        int accepted = (int) this.enrollments.stream()
                .filter(Enrollment::isAccepted)
                .count();
        return this.limitOfEnrollments - accepted;

    }

    public Long getNumberOfAcceptedEnrollments() {
        return this.enrollments.stream()
                .filter(Enrollment::isAccepted)
                .count();
    }

    public void updateFrom(EventForm eventForm) {
        this.title = eventForm.getTitle();
        this.description = eventForm.getDescription();
        this.eventType = eventForm.getEventType();
        this.startDateTime = eventForm.getStartDateTime();
        this.endDateTime = eventForm.getEndDateTime();
        this.limitOfEnrollments = eventForm.getLimitOfEnrollments();
        this.endEnrollmentDateTime = eventForm.getEndEnrollmentDateTime();
    }

    public boolean isAbleToAcceptWaitingEnrollment() { // 모임 유형이 선착순이고 모임 정원이 참가 요청 수보다 큰지 확인
        return this.eventType == EventType.FCFS && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments();
    }

    public void addEnrollment(Enrollment enrollment) {
        // 모임에 참가 내역을 추가하고 역으로 참가 내역에도 모임에 대한 레퍼런스를 추가해줘야 한다,
        this.enrollments.add(enrollment);
        enrollment.attach(this);
    }

    public void removeEnrollment(Enrollment enrollment) { // 모임에서 참가 내역을 제거 후  역으로 참가 내역에서도 모임
        this.enrollments.remove(enrollment);
        enrollment.detachEvent();
    }

    public void acceptNextIfAvailable() {
        // 대기중인 있는 참가 신청들이 수용가능한지 확인하여 첫 번째 신청을 가져와 참가 신청 상태로 변경
        if (this.isAbleToAcceptWaitingEnrollment()) {
            this.firstWaitingEnrollment().ifPresent(Enrollment::accept);
        }
    }

    private Optional<Enrollment> firstWaitingEnrollment() { // 참가 신청 중 신청 완료되지 않은 첫 번째 내역을 가져온다.
        return this.enrollments.stream()
                .filter(e -> !e.isAccepted())
                .findFirst();
    }

    public void acceptWaitingList() {
        // 대기중인 참가 신청 수용 가능 여부를 판단하여 참가 가능한 수만큼 대기 리스트의 상태를 참가 신청 완료 상태로 변경
        if (this.isAbleToAcceptWaitingEnrollment()) {
            List<Enrollment> waitingList = this.enrollments.stream()
                    .filter(e -> !e.isAccepted())
                    .collect(Collectors.toList());
            int numberToAccept = (int) Math.min(limitOfEnrollments - getNumberOfAcceptedEnrollments(), waitingList.size());
            waitingList.subList(0, numberToAccept).forEach(Enrollment::accept);
        }
    }
}

