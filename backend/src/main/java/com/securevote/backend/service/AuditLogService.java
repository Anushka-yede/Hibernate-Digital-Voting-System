package com.securevote.backend.service;

import com.securevote.backend.dto.AuditLogResponse;
import com.securevote.backend.entity.AuditLog;
import com.securevote.backend.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(String actor, String action, String details) {
        AuditLog log = new AuditLog();
        log.setActor(actor == null || actor.isBlank() ? "system" : actor);
        log.setAction(action);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(String actor, String action, Instant fromTime, Instant toTime, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return auditLogRepository.search(normalize(actor), normalize(action), fromTime, toTime,
                        PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(entry -> AuditLogResponse.builder()
                        .id(entry.getId())
                        .actor(entry.getActor())
                        .action(entry.getAction())
                        .details(entry.getDetails())
                        .createdAt(entry.getCreatedAt())
                        .build());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
