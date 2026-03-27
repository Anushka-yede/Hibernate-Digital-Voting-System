package com.securevote.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ElectionLifecycleNotification {
    private Long electionId;
    private String title;
    private String region;
    private String status;
    private Instant timestamp;
}
