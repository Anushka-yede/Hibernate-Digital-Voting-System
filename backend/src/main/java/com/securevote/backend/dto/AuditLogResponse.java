package com.securevote.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuditLogResponse {
    private Long id;
    private String actor;
    private String action;
    private String details;
    private Instant createdAt;
}
