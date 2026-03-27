package com.securevote.backend.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.securevote.backend.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:actor is null or lower(cast(a.actor as string)) like lower(concat('%', :actor, '%')))
              and (:action is null or lower(cast(a.action as string)) like lower(concat('%', :action, '%')))
              and (:fromTime is null or a.createdAt >= :fromTime)
              and (:toTime is null or a.createdAt <= :toTime)
            order by a.createdAt desc
            """)
    Page<AuditLog> search(@Param("actor") String actor,
                          @Param("action") String action,
                          @Param("fromTime") Instant fromTime,
                          @Param("toTime") Instant toTime,
                          Pageable pageable);
}
