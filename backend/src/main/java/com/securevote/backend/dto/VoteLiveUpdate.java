package com.securevote.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class VoteLiveUpdate {
    private Long electionId;
    private Map<String, Long> candidateVotes;
    private Instant updatedAt;
}
