package com.securevote.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiFraudResponse {
    private boolean suspicious;
    private double score;
    private String reason;
}
