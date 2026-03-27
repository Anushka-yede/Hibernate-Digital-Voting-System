package com.securevote.backend.dto;

import java.time.Instant;

import com.securevote.backend.entity.ElectionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElectionCreateRequest {
    @NotBlank
    private String title;

    @NotNull
    private ElectionType type;

    @NotBlank
    private String region;

    private String description;

    @NotNull
    private Instant startDate;

    @NotNull
    private Instant endDate;
}
