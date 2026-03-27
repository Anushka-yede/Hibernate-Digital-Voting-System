package com.securevote.backend.repository;

import com.securevote.backend.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    List<Candidate> findByElectionId(Long electionId);

    @Query("""
        select c from Candidate c
        where lower(c.name) like lower(concat('%', :name, '%'))
          and lower(c.party) like lower(concat('%', :party, '%'))
          and lower(c.region) like lower(concat('%', :region, '%'))
        """)
    Page<Candidate> search(
        @Param("name") String name,
        @Param("party") String party,
        @Param("region") String region,
        Pageable pageable
    );

    @Query("""
        select distinct c.region from Candidate c
        where c.region is not null and trim(c.region) <> ''
        order by c.region asc
        """)
    List<String> findDistinctRegions();
}
