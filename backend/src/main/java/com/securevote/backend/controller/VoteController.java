package com.securevote.backend.controller;

import com.securevote.backend.dto.VoteReceiptResponse;
import com.securevote.backend.dto.VoteRequest;
import com.securevote.backend.service.VoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
public class VoteController {

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping
    public ResponseEntity<VoteReceiptResponse> vote(Authentication authentication,
                                                    @Valid @RequestBody VoteRequest request) {
        return ResponseEntity.ok(voteService.castVote(authentication.getName(), request));
    }

    @GetMapping("/status")
    public ResponseEntity<java.util.Map<String, Boolean>> status(Authentication authentication,
                                                                 @RequestParam Long electionId) {
        boolean hasVoted = voteService.hasUserVoted(authentication.getName(), electionId);
        return ResponseEntity.ok(java.util.Map.of("hasVoted", hasVoted));
    }
}
