package io.lcalmsky.app.modules.event.application;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.event.domain.entity.Enrollment;
import io.lcalmsky.app.modules.event.domain.entity.Event;
import io.lcalmsky.app.modules.event.endpoint.form.EventForm;
import io.lcalmsky.app.modules.event.event.EnrollmentAcceptedEvent;
import io.lcalmsky.app.modules.event.infra.repository.EnrollmentRepository;
import io.lcalmsky.app.modules.event.infra.repository.EventRepository;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.event.StudyUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Event createEvent(Study study, EventForm eventForm, Account account) {
        Event event = Event.from(eventForm, account, study);
        eventPublisher.publishEvent(new StudyUpdateEvent(event.getStudy(), "'" + event.getTitle() + "' 모임이 생성되었습니다."));
        return eventRepository.save(event);
    }

    public void updateEvent(Event event, EventForm eventForm) {
        event.updateFrom(eventForm);
        event.acceptWaitingList(); // 모임 인원 수정시에도 반영될 수 있게 대기 목록에 있는 사용자들을 추가시켜 준다.
        eventPublisher.publishEvent(new StudyUpdateEvent(event.getStudy(), "'" + event.getTitle() + "' 모임이 정보가 수정되었습니다."));
    }

    public void deleteEvent(Event event) {
        eventRepository.delete(event);
        eventPublisher.publishEvent(new StudyUpdateEvent(event.getStudy(), "'" + event.getTitle() + "' 모임이 취소되었습니다."));
    }

    public void enroll(Event event, Account account) {
        if (!enrollmentRepository.existsByEventAndAccount(event, account)) { // 모임에 해당 계정이 참가한 내역이 있는지 확인한다.
            Enrollment enrollment = Enrollment.of(LocalDateTime.now(), event.isAbleToAcceptWaitingEnrollment(), account);
            // 참가 내역이 없으므로 참가 정보를 생성한다.
            event.addEnrollment(enrollment); // 모임에 참가 정보를 등록
            enrollmentRepository.save(enrollment); // 참가 정보를 저장

        }
    }

    public void leave(Event event, Account account) {
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account); // 참가 내역을 조회
        if (!enrollment.isAttended()) {
            event.removeEnrollment(enrollment); // 모임에서 참가 내역을 삭제
            enrollmentRepository.delete(enrollment);  // 참가 정보를 삭제
            event.acceptNextIfAvailable(); // 모임에서 다음 대기자를 참가 상태로 변경
        }
    }

    public void acceptEnrollment(Event event, Enrollment enrollment) {
        // 나머지 참가 신청/거절, 출석 체크/취소 기능은 entity에 위임
        event.accept(enrollment);
        eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment));
    }

    public void rejectEnrollment(Event event, Enrollment enrollment) {
        // 나머지 참가 신청/거절, 출석 체크/취소 기능은 entity에 위임
        event.reject(enrollment);
        eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment));
    }

    public void checkInEnrollment(Event event, Enrollment enrollment) {
        // 나머지 참가 신청/거절, 출석 체크/취소 기능은 entity에 위임
        enrollment.attend();

    }

    public void cancelCheckinEnrollment(Event event, Enrollment enrollment) {
        // 나머지 참가 신청/거절, 출석 체크/취소 기능은 entity에 위임
        enrollment.absent();
    }
}
