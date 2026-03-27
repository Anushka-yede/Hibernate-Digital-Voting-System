package com.securevote.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ElectionAnalyticsResponse {
    private Long electionId;
    private long totalVotes;
    private long totalEligibleVoters;
    private double participationRate;
    private List<CandidateVoteStat> candidateVotes;
    private List<RegionVoteStat> regionVotes;

    @Getter
    @Builder
    public static class CandidateVoteStat {
        private String candidateName;
        private String party;
        private long votes;
    }

    @Getter
    @Builder
    public static class RegionVoteStat {
        private String region;
        private long votes;
    }
}
