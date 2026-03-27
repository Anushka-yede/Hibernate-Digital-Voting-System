package com.securevote.backend.service;

import com.securevote.backend.dto.ElectionLifecycleNotification;
import com.securevote.backend.entity.Election;
import com.securevote.backend.entity.ElectionStatus;
import com.securevote.backend.repository.ElectionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ElectionLifecycleScheduler {

    private final ElectionRepository electionRepository;
    private final RealtimeEventService realtimeEventService;
    private final AuditLogService auditLogService;

    public ElectionLifecycleScheduler(ElectionRepository electionRepository,
                                      RealtimeEventService realtimeEventService,
                                      AuditLogService auditLogService) {
        this.electionRepository = electionRepository;
        this.realtimeEventService = realtimeEventService;
        this.auditLogService = auditLogService;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    @CacheEvict(cacheNames = {"elections:active", "elections:upcoming", "elections:all"}, allEntries = true)
    public void reconcileStatusAndNotify() {
        Instant now = Instant.now();
        List<Election> elections = electionRepository.findAll();

        for (Election election : elections) {
            ElectionStatus next = resolveStatus(election, now);
            if (next != election.getStatus()) {
                election.setStatus(next);
                electionRepository.save(election);

                realtimeEventService.publishLifecycle(ElectionLifecycleNotification.builder()
                        .electionId(election.getId())
                        .title(election.getTitle())
                        .region(election.getRegion())
                        .status(next.name())
                        .timestamp(now)
                        .build());

                auditLogService.log("system", "ELECTION_STATUS_CHANGED",
                        "Election " + election.getId() + " changed to " + next.name());
            }
        }
    }

    private ElectionStatus resolveStatus(Election election, Instant now) {
        if (now.isBefore(election.getStartDate())) {
            return ElectionStatus.DRAFT;
        }
        if (now.isAfter(election.getEndDate())) {
            return ElectionStatus.CLOSED;
        }
        return ElectionStatus.ACTIVE;
    }
}
