package com.frauddetection.notification.service;

import com.frauddetection.notification.dto.NotificationResponse;
import com.frauddetection.notification.entity.Notification;
import com.frauddetection.notification.entity.NotificationStatus;
import com.frauddetection.notification.repository.NotificationRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void sendNotification(@NonNull Notification notification) {
        notificationRepository.findByTransactionIdAndType(
                        notification.getTransactionId(),
                        notification.getType()
                )
                .orElseGet(() -> sendAndSave(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByAccount(@NonNull UUID accountId) {
        return notificationRepository.findByAccountIdOrderBySentAtDesc(accountId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    private Notification sendAndSave(Notification notification) {
        log.info("Sending email to account: {}", notification.getAccountId());
        notification.setSentAt(Instant.now());
        notification.setStatus(NotificationStatus.SENT);
        return notificationRepository.save(notification);
    }
}
