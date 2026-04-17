package com.frauddetection.dashboard.service;

import com.frauddetection.common.FraudAlertEvent;
import com.frauddetection.dashboard.dto.RecentFraudAlertResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardWebSocketPublisher {

    private static final String FRAUD_ALERTS_TOPIC = "/topic/fraud-alerts";
    private static final String DASHBOARD_STATS_TOPIC = "/topic/dashboard-stats";

    private final SimpMessagingTemplate messagingTemplate;
    private final DashboardStatsService dashboardStatsService;

    public void publishFraudAlert(FraudAlertEvent event) {
        messagingTemplate.convertAndSend(FRAUD_ALERTS_TOPIC, new RecentFraudAlertResponse(
                event.transactionId(),
                event.accountId(),
                event.reason(),
                event.riskScore(),
                event.detectedAt()
        ));
    }

    @Scheduled(
            fixedRateString = "${dashboard.websocket.stats-publish-rate-ms:5000}",
            initialDelayString = "${dashboard.websocket.stats-initial-delay-ms:5000}"
    )
    public void publishDashboardStats() {
        try {
            messagingTemplate.convertAndSend(DASHBOARD_STATS_TOPIC, dashboardStatsService.getStats());
        } catch (Exception exception) {
            log.error("Failed to publish dashboard stats over WebSocket", exception);
        }
    }
}
