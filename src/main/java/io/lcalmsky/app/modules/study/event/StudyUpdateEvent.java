package io.lcalmsky.app.modules.study.event;

import io.lcalmsky.app.modules.study.domain.entity.Study;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class StudyUpdateEvent {
    private final Study study;
    private final String message;
}

// 스터디 수정 시 발생시킬 이벤트 클래스를 생성

//study와 message를 생성자로 받을 수 있도록 @RequiredArgsConstructor를 사용하였고,
//이벤트 처리시 사용할 수 있게 @Getter를 추가하였다.