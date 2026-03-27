package com.securevote.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AiAlertResponse {
    private Long id;
    private Long userId;
    private Long electionId;
    private boolean suspicious;
    private double score;
    private String reason;
    private Instant createdAt;
}
