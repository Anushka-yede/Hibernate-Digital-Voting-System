package com.securevote.backend.controller;

import com.securevote.backend.dto.*;
import com.securevote.backend.service.AuditService;
import com.securevote.backend.service.AuditLogService;
import com.securevote.backend.service.ElectionAnalyticsService;
import com.securevote.backend.service.ElectionService;
import com.securevote.backend.service.VoteService;
import jakarta.validation.Valid;
import com.securevote.backend.entity.ElectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ElectionService electionService;
    private final VoteService voteService;
    private final AuditService auditService;
    private final AuditLogService auditLogService;
    private final ElectionAnalyticsService electionAnalyticsService;

    public AdminController(ElectionService electionService,
                           VoteService voteService,
                           AuditService auditService,
                           AuditLogService auditLogService,
                           ElectionAnalyticsService electionAnalyticsService) {
        this.electionService = electionService;
        this.voteService = voteService;
        this.auditService = auditService;
        this.auditLogService = auditLogService;
        this.electionAnalyticsService = electionAnalyticsService;
    }

    @GetMapping("/elections")
    public ResponseEntity<Page<ElectionResponse>> listElections(
            @RequestParam(required = false) ElectionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(electionService.listElections(status, page, size));
    }

    @PostMapping("/elections")
    public ResponseEntity<ElectionResponse> createElection(Authentication authentication,
                                                           @Valid @RequestBody ElectionCreateRequest request) {
        auditLogService.log(authentication.getName(), "ADMIN_CREATE_ELECTION", "title=" + request.getTitle());
        return ResponseEntity.ok(electionService.createElection(request));
    }

    @PutMapping("/elections/{electionId}")
    public ResponseEntity<ElectionResponse> updateElection(Authentication authentication,
                                                           @PathVariable Long electionId,
                                                           @Valid @RequestBody ElectionCreateRequest request) {
        auditLogService.log(authentication.getName(), "ADMIN_UPDATE_ELECTION", "electionId=" + electionId);
        return ResponseEntity.ok(electionService.updateElection(electionId, request));
    }

    @DeleteMapping("/elections/{electionId}")
    public ResponseEntity<Void> deleteElection(Authentication authentication, @PathVariable Long electionId) {
        auditLogService.log(authentication.getName(), "ADMIN_DELETE_ELECTION", "electionId=" + electionId);
        electionService.deleteElection(electionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/elections/{electionId}/candidates")
    public ResponseEntity<CandidateResponse> addCandidate(Authentication authentication,
                                                          @PathVariable Long electionId,
                                                          @Valid @RequestBody CandidateCreateRequest request) {
        auditLogService.log(authentication.getName(), "ADMIN_ADD_CANDIDATE", "electionId=" + electionId + ", name=" + request.getName());
        return ResponseEntity.ok(electionService.addCandidate(electionId, request));
    }

    @PostMapping("/elections/{electionId}/candidates/bulk")
    public ResponseEntity<java.util.List<CandidateResponse>> addCandidatesBulk(Authentication authentication,
                                                                                @PathVariable Long electionId,
                                                                                @Valid @RequestBody CandidateBulkCreateRequest request) {
        auditLogService.log(authentication.getName(), "ADMIN_BULK_ADD_CANDIDATE", "electionId=" + electionId + ", count=" + request.getCandidates().size());
        return ResponseEntity.ok(electionService.addCandidatesBulk(electionId, request));
    }

    @PutMapping("/candidates/{candidateId}")
    public ResponseEntity<CandidateResponse> updateCandidate(Authentication authentication,
                                                             @PathVariable Long candidateId,
                                                             @Valid @RequestBody CandidateCreateRequest request) {
        auditLogService.log(authentication.getName(), "ADMIN_UPDATE_CANDIDATE", "candidateId=" + candidateId);
        return ResponseEntity.ok(electionService.updateCandidate(candidateId, request));
    }

    @DeleteMapping("/candidates/{candidateId}")
    public ResponseEntity<Void> deleteCandidate(Authentication authentication, @PathVariable Long candidateId) {
        auditLogService.log(authentication.getName(), "ADMIN_DELETE_CANDIDATE", "candidateId=" + candidateId);
        electionService.deleteCandidate(candidateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/elections/{electionId}/results")
    public ResponseEntity<Map<String, Long>> electionResults(@PathVariable Long electionId) {
        return ResponseEntity.ok(voteService.getElectionResult(electionId));
    }

    @GetMapping("/elections/{electionId}/audit")
    public ResponseEntity<Map<String, Object>> auditElection(@PathVariable Long electionId) {
        return ResponseEntity.ok(auditService.verifyElection(electionId));
    }

    @GetMapping("/elections/{electionId}/analytics")
    public ResponseEntity<ElectionAnalyticsResponse> electionAnalytics(@PathVariable Long electionId) {
        return ResponseEntity.ok(electionAnalyticsService.getElectionAnalytics(electionId));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> auditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Instant fromTime,
            @RequestParam(required = false) Instant toTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(auditLogService.search(actor, action, fromTime, toTime, page, size));
    }
}
