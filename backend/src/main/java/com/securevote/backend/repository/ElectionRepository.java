package com.securevote.backend.repository;

import com.securevote.backend.entity.Election;
import com.securevote.backend.entity.ElectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface ElectionRepository extends JpaRepository<Election, Long> {
    Page<Election> findByStatus(ElectionStatus status, Pageable pageable);

    Page<Election> findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            ElectionStatus status,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    );

        Page<Election> findByStatusAndStartDateGreaterThan(
            ElectionStatus status,
            Instant startDate,
            Pageable pageable
        );

        @Query("""
            select distinct e
            from Election e
            left join fetch e.candidates c
            where e.status = :status and e.startDate <= :now and e.endDate >= :now
            order by e.startDate asc
            """)
        java.util.List<Election> findActiveWithCandidates(@Param("status") ElectionStatus status, @Param("now") Instant now);

        @Query("""
            select distinct e
            from Election e
            left join fetch e.candidates c
            where e.status = :status and e.startDate > :now
            order by e.startDate asc
            """)
        java.util.List<Election> findUpcomingWithCandidates(@Param("status") ElectionStatus status, @Param("now") Instant now);

            @Query("""
                select distinct e.region from Election e
                where e.region is not null and trim(e.region) <> ''
                order by e.region asc
                """)
            java.util.List<String> findDistinctRegions();
}
