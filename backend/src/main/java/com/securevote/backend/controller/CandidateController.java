package com.securevote.backend.controller;

import com.securevote.backend.dto.CandidateResponse;
import com.securevote.backend.service.ElectionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final ElectionService electionService;

    public CandidateController(ElectionService electionService) {
        this.electionService = electionService;
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CandidateResponse>> searchCandidates(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String party,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(electionService.searchCandidates(name, party, region, page, size));
    }

    @GetMapping("/regions")
    public ResponseEntity<java.util.List<String>> candidateRegions() {
        return ResponseEntity.ok(electionService.listCandidateRegions());
    }
}
