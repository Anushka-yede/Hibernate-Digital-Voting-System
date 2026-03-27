package com.securevote.backend.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.securevote.backend.dto.AiFraudRequest;
import com.securevote.backend.dto.AiFraudResponse;
import com.securevote.backend.dto.VoteLiveUpdate;
import com.securevote.backend.dto.VoteReceiptResponse;
import com.securevote.backend.dto.VoteRequest;
import com.securevote.backend.entity.Candidate;
import com.securevote.backend.entity.Election;
import com.securevote.backend.entity.User;
import com.securevote.backend.entity.Vote;
import com.securevote.backend.exception.BusinessException;
import com.securevote.backend.exception.ResourceNotFoundException;
import com.securevote.backend.repository.CandidateRepository;
import com.securevote.backend.repository.ElectionRepository;
import com.securevote.backend.repository.UserRepository;
import com.securevote.backend.repository.VoteRepository;

@Service
public class VoteService {

    private static final Logger log = LoggerFactory.getLogger(VoteService.class);

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;
    private final BlockchainService blockchainService;
    private final AiMonitoringService aiMonitoringService;
    private final RealtimeEventService realtimeEventService;
    private final AuditLogService auditLogService;

    public VoteService(VoteRepository voteRepository,
                       UserRepository userRepository,
                       ElectionRepository electionRepository,
                       CandidateRepository candidateRepository,
                       BlockchainService blockchainService,
                       AiMonitoringService aiMonitoringService,
                       RealtimeEventService realtimeEventService,
                       AuditLogService auditLogService) {
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
        this.blockchainService = blockchainService;
        this.aiMonitoringService = aiMonitoringService;
        this.realtimeEventService = realtimeEventService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    @CacheEvict(cacheNames = {"elections:active", "elections:all", "analytics"}, allEntries = true)
    public VoteReceiptResponse castVote(String username, VoteRequest request) {
        User user = userRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Election election = electionRepository.findById(request.getElectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));
        Candidate candidate = candidateRepository.findById(request.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        Instant now = Instant.now();

        if (election.getStartDate().isAfter(now) || election.getEndDate().isBefore(now)) {
            throw new BusinessException("Election is not active");
        }

        if (!candidate.getElection().getId().equals(election.getId())) {
            throw new BusinessException("Candidate does not belong to election");
        }

        if (voteRepository.existsByVoterIdAndElectionId(user.getId(), election.getId())) {
            aiMonitoringService.storeMultipleVoteAttemptAlert(
                    user.getId(),
                    election.getId(),
                    "User tried to vote again in the same election"
            );
            throw new BusinessException("You have already voted in this election");
        }

        AiFraudRequest fraudRequest = AiFraudRequest.builder()
                .userId(user.getId())
                .electionId(election.getId())
            .currentTimestamp(now.toEpochMilli())
            .build();

        AiFraudResponse fraudResponse = aiMonitoringService.checkFraud(fraudRequest);

        if (fraudResponse.isSuspicious() && fraudResponse.getScore() > 0.92) {
            auditLogService.log(username, "VOTE_BLOCKED", "Vote blocked for election=" + election.getId() + " reason=" + fraudResponse.getReason());
            throw new BusinessException("Vote blocked due to high fraud risk");
        }

        String voteHash = blockchainService.generateVoteHash(user.getId(), candidate.getId(), now);
        String txHash = blockchainService.storeVoteHashOnChain(voteHash);

        Vote vote = new Vote();
        vote.setVoter(user);
        vote.setElection(election);
        vote.setCandidate(candidate);
        vote.setVoteHash(voteHash);
        vote.setBlockchainTxHash(txHash);
        vote.setCastAt(now);

        Vote saved;
        try {
            saved = voteRepository.save(vote);
        } catch (DataIntegrityViolationException ex) {
            aiMonitoringService.storeMultipleVoteAttemptAlert(
                    user.getId(),
                    election.getId(),
                    "Concurrent duplicate vote attempt detected"
            );
            throw new BusinessException("You have already voted in this election");
        }

        log.info("Vote cast by user={} election={} candidate={} txHash={}", user.getId(), election.getId(), candidate.getId(), txHash);
        auditLogService.log(username, "VOTE_CAST",
            "election=" + election.getId() + ", candidate=" + candidate.getId() + ", txHash=" + txHash);

        realtimeEventService.publishVoteUpdate(VoteLiveUpdate.builder()
            .electionId(election.getId())
            .candidateVotes(getElectionResult(election.getId()))
            .updatedAt(Instant.now())
            .build());

        realtimeEventService.publishUserNotification(username, Map.of(
            "type", "VOTE_SUCCESS",
            "electionId", election.getId(),
            "candidateId", candidate.getId(),
            "blockchainTxHash", txHash,
            "timestamp", Instant.now().toString()
        ));

        return VoteReceiptResponse.builder()
                .voteId(saved.getId())
                .electionId(election.getId())
                .candidateId(candidate.getId())
                .voteHash(voteHash)
                .blockchainTxHash(txHash)
                .castAt(saved.getCastAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getElectionResult(Long electionId) {
        Map<String, Long> result = new LinkedHashMap<>();
        voteRepository.countVotesByCandidateName(electionId)
                .forEach(r -> result.put(String.valueOf(r[0]), ((Number) r[2]).longValue()));
        return result;
    }

    @Transactional(readOnly = true)
    public boolean hasUserVoted(String username, Long electionId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return voteRepository.existsByVoterIdAndElectionId(user.getId(), electionId);
    }
}
