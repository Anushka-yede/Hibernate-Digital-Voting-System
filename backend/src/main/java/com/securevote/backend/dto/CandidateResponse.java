package com.securevote.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CandidateResponse {
    private Long id;
    private String name;
    private String party;
    private String region;
    private String manifesto;
    private Long electionId;
}
