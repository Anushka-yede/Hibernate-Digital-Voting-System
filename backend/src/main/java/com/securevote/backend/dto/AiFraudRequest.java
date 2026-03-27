package com.securevote.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiFraudRequest {
    private Long userId;
    private Long electionId;
    private long currentTimestamp;
}
