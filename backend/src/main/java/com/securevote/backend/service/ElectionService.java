package com.securevote.backend.service;

import com.securevote.backend.dto.*;
import com.securevote.backend.entity.Candidate;
import com.securevote.backend.entity.Election;
import com.securevote.backend.entity.ElectionStatus;
import com.securevote.backend.exception.BusinessException;
import com.securevote.backend.exception.ResourceNotFoundException;
import com.securevote.backend.repository.CandidateRepository;
import com.securevote.backend.repository.ElectionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ElectionService {

    private final ElectionRepository electionRepository;
    private final CandidateRepository candidateRepository;

    public ElectionService(ElectionRepository electionRepository, CandidateRepository candidateRepository) {
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
    }

    @Transactional
    @CacheEvict(cacheNames = {"elections:active", "elections:upcoming", "elections:all", "candidates:search"}, allEntries = true)
    public ElectionResponse createElection(ElectionCreateRequest request) {
        validateElectionDates(request.getStartDate(), request.getEndDate());

        Election election = new Election();
        election.setTitle(request.getTitle());
        election.setType(request.getType());
        election.setRegion(request.getRegion());
        election.setDescription(request.getDescription());
        election.setStartDate(request.getStartDate());
        election.setEndDate(request.getEndDate());
        election.setStatus(resolveStatus(request.getStartDate(), request.getEndDate()));

        Election saved = electionRepository.save(election);
        return toResponse(saved, List.of());
    }

    @Transactional
    @CacheEvict(cacheNames = {"elections:active", "elections:upcoming", "elections:all", "candidates:search"}, allEntries = true)
    public ElectionResponse updateElection(Long electionId, ElectionCreateRequest request) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));

        validateElectionDates(request.getStartDate(), request.getEndDate());

        election.setTitle(request.getTitle());
        election.setType(request.getType());
        election.setRegion(request.getRegion());
        election.setDescription(request.getDescription());
        election.setStartDate(request.getStartDate());
        election.setEndDate(request.getEndDate());
        election.setStatus(resolveStatus(request.getStartDate(), request.getEndDate()));

        Election saved = electionRepository.save(election);
        List<CandidateResponse> candidates = candidateRepository.findByElectionId(saved.getId())
                .stream()
                .map(this::toCandidateResponse)
                .toList();
        return toResponse(saved, candidates);
    }

    @Transactional
    @CacheEvict(cacheNames = {"elections:active", "elections:upcoming", "elections:all", "candidates:search"}, allEntries = true)
    public void deleteElection(Long electionId) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));
        electionRepository.delete(election);
    }

    @Transactional
    @CacheEvict(cacheNames = {"elections:active", "elections:upcoming", "elections:all", "candidates:search"}, allEntries = true)
    public CandidateResponse addCandidate(Long electionId, CandidateCreateRequest request) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));

        validateCandidateRegionAgainstElection(election, request.getRegion());

        Candidate candidate = new Candidate();
        candidate.setName(request.getName());
        candidate.setParty(request.getParty());
        candidate.setRegion(request.getRegion());
        candidate.setManifesto(request.getManifesto());
        candidate.setElection(election);

        Candidate saved = candidateRepository.save(candidate);
        return toCandidateResponse(saved);
        }

    @Transactional
    @CacheEvict(cacheNames = {"elections:active", "elections:upcoming", "elections:all", "candidates:search"}, allEntries = true)
    public List<CandidateResponse> addCandidatesBulk(Long electionId, CandidateBulkCreateRequest request) {
        Election election = electionRepository.findById(electionId)
                .orElseThrow(() -> new ResourceNotFoundException("Election not found"));

        return request.getCandidates().stream()
                .map(candidateRequest -> {
                    validateCandidateRegionAgainstElection(election, candidateRequest.getRegion());

                    Candidate candidate = new Candidate();
                    candidate.setName(candidateRequest.getName());
                    candidate.setParty(candidateRequest.getParty());
                    candidate.setRegion(candidateRequest.getRegion());
                    candidate.setManifesto(candidateRequest.getManifesto());
                    candidate.setElection(election);
                    return candidateRepository.save(candidate);
                })
                .map(this::toCandidateResponse)
                .toList();
    }

        @Transactional
        @CacheEvict(cacheNames = {"elections:active", "elections:upcoming", "elections:all", "candidates:search"}, allEntries = true)
        public CandidateResponse updateCandidate(Long candidateId, CandidateCreateRequest request) {
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        candidate.setName(request.getName());
        candidate.setParty(request.getParty());
        candidate.setRegion(request.getRegion());
        candidate.setManifesto(request.getManifesto());

        Candidate saved = candidateRepository.save(candidate);
        return toCandidateResponse(saved);
        }

        @Transactional
        @CacheEvict(cacheNames = {"elections:active", "elections:upcoming", "elections:all", "candidates:search"}, allEntries = true)
        public void deleteCandidate(Long candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        candidateRepository.delete(candidate);
        }

        @Transactional(readOnly = true)
        public Page<CandidateResponse> searchCandidates(String name, String party, String region, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return candidateRepository.search(normalizeFilter(name), normalizeFilter(party), normalizeFilter(region), pageable)
            .map(this::toCandidateResponse);
    }

    @Transactional(readOnly = true)
    public Page<ElectionResponse> listElections(ElectionStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Election> elections = status == null
                ? electionRepository.findAll(pageable)
                : electionRepository.findByStatus(status, pageable);

        return elections.map(e -> {
            List<CandidateResponse> candidates = candidateRepository.findByElectionId(e.getId())
                    .stream()
                    .map(this::toCandidateResponse)
                    .toList();
            return toResponse(e, candidates);
        });
    }

    @Transactional(readOnly = true)
    public Page<ElectionResponse> listActiveElections(int page, int size) {
        Instant now = Instant.now();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.ASC, "startDate"));
        List<ElectionResponse> rows = electionRepository.findActiveWithCandidates(ElectionStatus.ACTIVE, now)
                .stream()
                .skip((long) page * safeSize)
                .limit(safeSize)
                .map(e -> toResponse(e, e.getCandidates().stream().map(this::toCandidateResponse).toList()))
                .toList();
        long total = electionRepository.findByStatus(ElectionStatus.ACTIVE, PageRequest.of(0, 1)).getTotalElements();
        return new PageImpl<>(rows, pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<ElectionResponse> listUpcomingElections(int page, int size) {
        Instant now = Instant.now();
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.ASC, "startDate"));
        List<ElectionResponse> rows = electionRepository.findUpcomingWithCandidates(ElectionStatus.DRAFT, now)
                .stream()
                .skip((long) page * safeSize)
                .limit(safeSize)
                .map(e -> toResponse(e, e.getCandidates().stream().map(this::toCandidateResponse).toList()))
                .toList();
        long total = electionRepository.findByStatus(ElectionStatus.DRAFT, PageRequest.of(0, 1)).getTotalElements();
        return new PageImpl<>(rows, pageable, total);
    }

    @Transactional(readOnly = true)
    public List<String> listCandidateRegions() {
        return candidateRepository.findDistinctRegions();
    }

    @Transactional(readOnly = true)
    public List<String> listElectionRegions() {
        return electionRepository.findDistinctRegions();
    }

    private ElectionResponse toResponse(Election election, List<CandidateResponse> candidates) {
        return ElectionResponse.builder()
                .id(election.getId())
                .title(election.getTitle())
                .type(election.getType())
                .region(election.getRegion())
                .description(election.getDescription())
                .status(election.getStatus())
                .startDate(election.getStartDate())
                .endDate(election.getEndDate())
                .candidates(candidates)
                .build();
    }

    private CandidateResponse toCandidateResponse(Candidate candidate) {
        return CandidateResponse.builder()
                .id(candidate.getId())
                .name(candidate.getName())
                .party(candidate.getParty())
                .region(candidate.getRegion())
                .manifesto(candidate.getManifesto())
                .electionId(candidate.getElection().getId())
                .build();
    }

    private ElectionStatus resolveStatus(Instant startDate, Instant endDate) {
        Instant now = Instant.now();
        if (now.isBefore(startDate)) {
            return ElectionStatus.DRAFT;
        }
        if (now.isAfter(endDate)) {
            return ElectionStatus.CLOSED;
        }
        return ElectionStatus.ACTIVE;
    }

    private void validateElectionDates(Instant startDate, Instant endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new BusinessException("Election end date must be after start date");
        }
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private void validateCandidateRegionAgainstElection(Election election, String candidateRegion) {
        if (candidateRegion == null || candidateRegion.isBlank()) {
            throw new BusinessException("Candidate region is required");
        }

        String electionRegion = election.getRegion() == null ? "" : election.getRegion().trim().toLowerCase();
        String candidateRegionNormalized = candidateRegion.trim().toLowerCase();

        if (!electionRegion.isBlank() && !"india".equals(electionRegion)
                && !candidateRegionNormalized.contains(electionRegion)) {
            throw new BusinessException("Candidate region must include election region: " + election.getRegion());
        }
    }
}
