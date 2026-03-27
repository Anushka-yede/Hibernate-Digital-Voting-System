package com.securevote.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RealtimeAlertEvent {
    private Long alertId;
    private Long userId;
    private Long electionId;
    private double score;
    private String reason;
    private boolean suspicious;
    private Instant createdAt;
}
