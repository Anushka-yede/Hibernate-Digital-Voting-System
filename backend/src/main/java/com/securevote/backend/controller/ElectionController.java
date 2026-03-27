package com.securevote.backend.controller;

import com.securevote.backend.dto.ElectionResponse;
import com.securevote.backend.service.ElectionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/elections")
public class ElectionController {

    private final ElectionService electionService;

    public ElectionController(ElectionService electionService) {
        this.electionService = electionService;
    }

    @GetMapping
    public ResponseEntity<Page<ElectionResponse>> listElections(
            @RequestParam(defaultValue = "active") String scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if ("upcoming".equalsIgnoreCase(scope)) {
            return ResponseEntity.ok(electionService.listUpcomingElections(page, size));
        }

        if ("all".equalsIgnoreCase(scope)) {
            return ResponseEntity.ok(electionService.listElections(null, page, size));
        }

        return ResponseEntity.ok(electionService.listActiveElections(page, size));
    }

    @GetMapping("/regions")
    public ResponseEntity<java.util.List<String>> electionRegions() {
        return ResponseEntity.ok(electionService.listElectionRegions());
    }
}
