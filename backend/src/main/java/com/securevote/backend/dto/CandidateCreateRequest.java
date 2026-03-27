package com.securevote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String party;

    @NotBlank
    private String region;

    private String manifesto;
}
