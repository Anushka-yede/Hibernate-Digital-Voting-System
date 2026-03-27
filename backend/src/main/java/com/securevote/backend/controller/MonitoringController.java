package com.securevote.backend.controller;

import com.securevote.backend.service.AiMonitoringService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    private final AiMonitoringService aiMonitoringService;

    public MonitoringController(AiMonitoringService aiMonitoringService) {
        this.aiMonitoringService = aiMonitoringService;
    }

    @GetMapping("/anomaly-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> anomalySummary() {
        return ResponseEntity.ok(aiMonitoringService.fetchAnomalySummary());
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<com.securevote.backend.dto.AiAlertResponse>> alerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(aiMonitoringService.listAlerts(page, Math.min(Math.max(size, 1), 100)));
    }
}
