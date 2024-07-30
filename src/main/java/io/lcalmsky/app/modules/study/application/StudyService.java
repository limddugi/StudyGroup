package io.lcalmsky.app.modules.study.application;

import io.lcalmsky.app.modules.account.domain.entity.Account;
import io.lcalmsky.app.modules.account.domain.entity.Zone;
import io.lcalmsky.app.modules.study.domain.entity.Study;
import io.lcalmsky.app.modules.study.endpoint.form.StudyDescriptionForm;
import io.lcalmsky.app.modules.study.endpoint.form.StudyForm;
import io.lcalmsky.app.modules.study.event.StudyCreatedEvent;
import io.lcalmsky.app.modules.study.event.StudyUpdateEvent;
import io.lcalmsky.app.modules.study.infra.repostiory.StudyRepository;
import io.lcalmsky.app.modules.tag.domain.entity.Tag;
import io.lcalmsky.app.modules.tag.infra.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.utility.RandomString;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyService {

    private final StudyRepository studyRepository;
    private final ApplicationEventPublisher eventPublisher; // 이벤트를 발생시키기 위해 빈을 주입
    // CRUD 작업을 위해 StudyRepository를 주입
    private final TagRepository tagRepository;

    public Study createNewStudy(StudyForm studyForm, Account account) {
        // StudyForm과 현재 로그인중인 Account 정보를 전달받아 Study를 생성.
        // Study 생성 후에는 생성한 계정을 관리자로 등록해주고 DB에 저장
        Study study = Study.from(studyForm);
        study.addManager(account);
        // 스터디가 만들어지는 시점에 이벤트를 발생시킨다. 맨 처음에 다뤘듯이 비동기처리(다른 스레드에서 처리)를 하지 않으면
        // 여기서 RuntimeException이 발생했을 경우 @Transactional의 영향을 받게되어 rollback이 발생하므로 주의해야 한다.
        return studyRepository.save(study);
    }

    public Study getStudy(String path) { // 일반 사용자의 접근과 관리자가 수정하기 위해 접근할 때를 구분해 주었다.
        Study study = studyRepository.findByPath(path);
        checkStudyExists(path, study);
        return study;
    }

    public Study getStudyToUpdate(Account account, String path) { // 일반 사용자의 접근과 관리자가 수정하기 위해 접근할 때를 구분해 주었다.
        return getStudy(account, path, studyRepository.findByPath(path));
    }

    public Study getStudyToUpdateTag(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithTagsByPath(path));
    }

    public Study getStudyToUpdateZone(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithZonesByPath(path));
    }

    public Study getStudyToUpdateStatus(Account account, String path) {
        return getStudy(account, path, studyRepository.findStudyWithManagersByPath(path));
    }

    private Study getStudy(Account account, String path, Study studyByPath) {
        checkStudyExists(path, studyByPath);
        checkAccountIsManager(account, studyByPath);
        return studyByPath;
    }

    private void checkStudyExists(String path, Study study) {
        if (study == null) {
            throw new IllegalArgumentException(path + "에 해당하는 스터디가 없습니다.");
        }
    }

    private void checkAccountIsManager(Account account, Study study) {
        if (!study.isManagedBy(account)) {
            throw new AccessDeniedException("해당 기능을 사용할 수 없습니다.");
        }
    }

    public void updateStudyDescription(Study study, StudyDescriptionForm studyDescriptionForm) {
        // 스터디 소개 관련 파라미터를 전달받아 업데이트 한다.
        study.updateDescription(studyDescriptionForm);
        eventPublisher.publishEvent(new StudyUpdateEvent(study, "스터디 소개를 수정했습니다."));
    }

    public void updateStudyImage(Study study, String image) {
        study.updateImage(image);
    }

    public void enableStudyBanner(Study study) {
        study.setBanner(true);
    }

    public void disableStudyBanner(Study study) {
        study.setBanner(false);
    }

    public void addTag(Study study, Tag tag) {
        study.addTag(tag);
    }

    public void removeTag(Study study, Tag tag) {
        study.removeTag(tag);
    }

    public void addZone(Study study, Zone zone) {
        // 중복된 Zone 추가를 방지
        if (!study.getZones().contains(zone)) {
            study.addZone(zone);
        }
    }

    public void removeZone(Study study, Zone zone) {
        study.removeZone(zone);
    }

    public void publish(Study study) { // 스터디를 공개
        study.publish();
        eventPublisher.publishEvent(new StudyCreatedEvent(study));
    }

    public void close(Study study) { // 스터디를 종료
        study.close();
        eventPublisher.publishEvent(new StudyUpdateEvent(study, "스터디를 종료했습니다."));
    }

    public void startRecruit(Study study) { // 팀원 모집을 시작
        study.startRecruit();
        eventPublisher.publishEvent(new StudyUpdateEvent(study, "팀원 모집을 시작합니다."));
    }

    public void stopRecruit(Study study) { // 팀원 모집을 중단
        study.stopRecruit();
        eventPublisher.publishEvent(new StudyUpdateEvent(study, "팀원 모집을 종료했습니다."));
    }

    public boolean isValidPath(String newPath) {
        // 스터디 경로가 유효한지 판다한다. 정규표현식을 이용한 패턴 검사는 StudyForm에서 사용한 패턴을 상수로 추출하여 동일하게 사용하였다.
        //기존에 존재하는 경로를 사용해선 안 되므로 해당 경로를 사용하는 스터디가 있는디고 확인해준다.
        if (!newPath.matches(StudyForm.VALID_PATH_PATTERN)) {
            return false;
        }
        return !studyRepository.existsByPath(newPath);
    }

    public void updateStudyPath(Study study, String newPath) { // 스터디 경로를 업데이트 해준다.
        study.updatePath(newPath);
    }

    public boolean isValidTitle(String newTitle) { // 스터디 이름의 유효성을 검사한다.
        return newTitle.length() <= 50;
    }

    public void updateStudyTitle(Study study, String newTitle) { // 스터디 이름을 업데이트 한다.
        study.updateTitle(newTitle);
    }

    public void remove(Study study) { // 스터디를 삭제한다.
        if (!study.isRemovable()) {
            throw new IllegalStateException("스터디를 삭제할 수 없습니다.");
        }
        studyRepository.delete(study);
    }

    public void addMember(Study study, Account account) {
        study.addMember(account);

    }

    public void removeMember(Study study, Account account) {
        study.removeMember(account);
    }

    public Study getStudyToEnroll(String path) {
        return studyRepository.findStudyOnlyByPath(path)
                .orElseThrow(() -> new IllegalArgumentException(path + "에 해당하는 스터디가 존재하지 않습니다."));
    }
}
