package com.securevote.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class VoteReceiptResponse {
    private Long voteId;
    private Long electionId;
    private Long candidateId;
    private String voteHash;
    private String blockchainTxHash;
    private Instant castAt;
}
