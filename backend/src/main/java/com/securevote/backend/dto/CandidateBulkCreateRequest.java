package com.securevote.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CandidateBulkCreateRequest {

    @NotEmpty
    private List<@Valid CandidateCreateRequest> candidates;
}
