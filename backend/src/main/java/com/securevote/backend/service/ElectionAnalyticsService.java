package com.securevote.backend.service;

import com.securevote.backend.dto.ElectionAnalyticsResponse;
import com.securevote.backend.entity.Election;
import com.securevote.backend.exception.ResourceNotFoundException;
import com.securevote.backend.repository.ElectionRepository;
import com.securevote.backend.repository.UserRepository;
import com.securevote.backend.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ElectionAnalyticsService {

    private final VoteRepository voteRepository;
    private final ElectionRepository electionRepository;
    private final UserRepository userRepository;

    public ElectionAnalyticsService(VoteRepository voteRepository,
                                    ElectionRepository electionRepository,
                                    UserRepository userRepository) {
        this.voteRepository = voteRepository;
        this.electionRepository = electionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ElectionAnalyticsResponse getElectionAnalytics(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));

        long totalVotes = voteRepository.countByElectionId(electionId);
        long eligible = userRepository.count();
        double participationRate = eligible == 0 ? 0.0 : (double) totalVotes * 100.0 / eligible;

        List<ElectionAnalyticsResponse.CandidateVoteStat> candidateVotes = voteRepository.countVotesByCandidateName(electionId)
                .stream()
                .map(row -> ElectionAnalyticsResponse.CandidateVoteStat.builder()
                        .candidateName(String.valueOf(row[0]))
                        .party(String.valueOf(row[1]))
                        .votes(((Number) row[2]).longValue())
                        .build())
                .toList();

        List<ElectionAnalyticsResponse.RegionVoteStat> regionVotes = voteRepository.countVotesByRegion(electionId)
                .stream()
                .map(row -> ElectionAnalyticsResponse.RegionVoteStat.builder()
                        .region(String.valueOf(row[0]))
                        .votes(((Number) row[1]).longValue())
                        .build())
                .toList();

        return ElectionAnalyticsResponse.builder()
                .electionId(election.getId())
                .totalVotes(totalVotes)
                .totalEligibleVoters(eligible)
                .participationRate(participationRate)
                .candidateVotes(candidateVotes)
                .regionVotes(regionVotes)
                .build();
    }
}
