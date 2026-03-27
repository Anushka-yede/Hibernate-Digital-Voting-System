package com.securevote.backend.dto;

import com.securevote.backend.entity.ElectionStatus;
import com.securevote.backend.entity.ElectionType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ElectionResponse {
    private Long id;
    private String title;
    private ElectionType type;
    private String region;
    private String description;
    private ElectionStatus status;
    private Instant startDate;
    private Instant endDate;
    private List<CandidateResponse> candidates;
}
