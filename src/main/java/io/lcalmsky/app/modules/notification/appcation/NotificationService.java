package io.lcalmsky.app.modules.notification.appcation;

import io.lcalmsky.app.modules.notification.domain.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {
    public void markAsRead(List<Notification> notifications) {
        notifications.forEach(Notification::read);
    }
}

// 읽지 않은 알림 리스트를 전달받아 Notification Entity에서 읽은 상태로 직접 바꾸도록 위임한다.
