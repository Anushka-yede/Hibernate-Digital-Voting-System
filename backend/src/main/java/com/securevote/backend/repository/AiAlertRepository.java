package com.securevote.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.securevote.backend.entity.AiAlert;

public interface AiAlertRepository extends JpaRepository<AiAlert, Long> {

        @Query("""
                        select a from AiAlert a
                        where lower(a.reason) like '%vote%'
                            and lower(a.reason) like '%attempt%'
                        order by a.createdAt desc
                        """)
        Page<AiAlert> findVoteAttemptAlerts(Pageable pageable);
}
