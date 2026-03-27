package com.securevote.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoteRequest {
    @NotNull
    @Positive
    private Long electionId;

    @NotNull
    @Positive
    private Long candidateId;
}
