package com.frauddetection.notification.controller;

import com.frauddetection.common.ApiResponse;
import com.frauddetection.notification.dto.NotificationResponse;
import com.frauddetection.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/account/{accountId}")
    public ApiResponse<List<NotificationResponse>> getNotificationsByAccount(@PathVariable UUID accountId) {
        return ApiResponse.success(notificationService.getNotificationsByAccount(accountId));
    }
}
