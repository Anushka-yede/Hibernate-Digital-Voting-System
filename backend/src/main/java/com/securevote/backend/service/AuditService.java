package com.securevote.backend.service;

import com.securevote.backend.entity.Vote;
import com.securevote.backend.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditService {

    private final VoteRepository voteRepository;
    private final BlockchainService blockchainService;

    public AuditService(VoteRepository voteRepository, BlockchainService blockchainService) {
        this.voteRepository = voteRepository;
        this.blockchainService = blockchainService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifyElection(Long electionId) {
        List<Vote> votes = voteRepository.findByElectionId(electionId);
        int total = votes.size();
        int verified = 0;
        java.util.List<Map<String, Object>> mismatches = new java.util.ArrayList<>();

        for (Vote vote : votes) {
            boolean ok = blockchainService.verifyAnchoredWithHash(vote.getBlockchainTxHash(), vote.getVoteHash());
            if (ok) {
                verified++;
            } else {
                mismatches.add(Map.of(
                        "voteId", vote.getId(),
                        "candidateId", vote.getCandidate().getId(),
                        "txHash", vote.getBlockchainTxHash(),
                        "castAt", vote.getCastAt()
                ));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("electionId", electionId);
        result.put("totalVotes", total);
        result.put("verifiedOnChain", verified);
        result.put("unverified", total - verified);
        result.put("mismatches", mismatches);
        return result;
    }
}
