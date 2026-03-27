package com.securevote.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.securevote.backend.dto.AiAlertResponse;
import com.securevote.backend.dto.AiFraudRequest;
import com.securevote.backend.dto.AiFraudResponse;
import com.securevote.backend.dto.RealtimeAlertEvent;
import com.securevote.backend.entity.AiAlert;
import com.securevote.backend.repository.AiAlertRepository;

@Service
public class AiMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(AiMonitoringService.class);

    private final RestTemplate restTemplate;
    private final AiAlertRepository aiAlertRepository;
    private final RealtimeEventService realtimeEventService;
    private final AuditLogService auditLogService;

    @Value("${ai.service-url}")
    private String aiServiceUrl;

    public AiMonitoringService(RestTemplate restTemplate,
                               AiAlertRepository aiAlertRepository,
                               RealtimeEventService realtimeEventService,
                               AuditLogService auditLogService) {
        this.restTemplate = restTemplate;
        this.aiAlertRepository = aiAlertRepository;
        this.realtimeEventService = realtimeEventService;
        this.auditLogService = auditLogService;
    }

    public AiFraudResponse checkFraud(AiFraudRequest request) {
        try {
            ResponseEntity<AiFraudResponse> response = restTemplate.exchange(
                    aiServiceUrl + "/fraud/check",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    AiFraudResponse.class
            );
            return response.getBody() == null ? defaultResponse() : response.getBody();
        } catch (Exception ex) {
            log.warn("AI fraud service unavailable, defaulting to non-blocking decision", ex);
            return defaultResponse();
        }
    }

    public Object fetchAnomalySummary() {
        try {
            return restTemplate.getForObject(aiServiceUrl + "/anomaly/summary", Object.class);
        } catch (Exception ex) {
            return java.util.Map.of("status", "unavailable", "message", "AI anomaly service is unreachable");
        }
    }

    public void storeAlert(AiFraudRequest request, AiFraudResponse response) {
        if (response == null || (!response.isSuspicious() && response.getScore() < 0.6)) {
            return;
        }

        AiAlert alert = new AiAlert();
        alert.setUserId(request.getUserId());
        alert.setElectionId(request.getElectionId());
        alert.setSuspicious(response.isSuspicious());
        alert.setScore(response.getScore());
        alert.setReason(response.getReason() == null ? "unknown" : response.getReason());
        AiAlert saved = aiAlertRepository.save(alert);

        realtimeEventService.publishAlert(RealtimeAlertEvent.builder()
                .alertId(saved.getId())
                .userId(saved.getUserId())
                .electionId(saved.getElectionId())
                .score(saved.getScore())
                .reason(saved.getReason())
                .suspicious(saved.isSuspicious())
                .createdAt(saved.getCreatedAt())
                .build());

        if (saved.isSuspicious() || saved.getScore() >= 0.8) {
            auditLogService.log("system", "AI_ALERT", "user=" + saved.getUserId() + ", election=" + saved.getElectionId() + ", score=" + saved.getScore());
        }
    }

    public void storeMultipleVoteAttemptAlert(Long userId, Long electionId, String reason) {
        AiAlert alert = new AiAlert();
        alert.setUserId(userId);
        alert.setElectionId(electionId);
        alert.setSuspicious(true);
        alert.setScore(1.0);
        alert.setReason(reason == null || reason.isBlank() ? "Multiple vote attempt detected" : reason);

        AiAlert saved = aiAlertRepository.save(alert);

        realtimeEventService.publishAlert(RealtimeAlertEvent.builder()
                .alertId(saved.getId())
                .userId(saved.getUserId())
                .electionId(saved.getElectionId())
                .score(saved.getScore())
                .reason(saved.getReason())
                .suspicious(saved.isSuspicious())
                .createdAt(saved.getCreatedAt())
                .build());

        auditLogService.log("system", "MULTIPLE_VOTE_ATTEMPT",
                "user=" + saved.getUserId() + ", election=" + saved.getElectionId() + ", reason=" + saved.getReason());
    }

    public Page<AiAlertResponse> listAlerts(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return aiAlertRepository.findVoteAttemptAlerts(PageRequest.of(page, safeSize))
                .map(alert -> AiAlertResponse.builder()
                        .id(alert.getId())
                        .userId(alert.getUserId())
                        .electionId(alert.getElectionId())
                        .suspicious(alert.isSuspicious())
                        .score(alert.getScore())
                        .reason(alert.getReason())
                        .createdAt(alert.getCreatedAt())
                        .build());
    }

    private AiFraudResponse defaultResponse() {
        AiFraudResponse resp = new AiFraudResponse();
        resp.setSuspicious(false);
        resp.setScore(0.0);
        resp.setReason("AI service fallback");
        return resp;
    }
}
