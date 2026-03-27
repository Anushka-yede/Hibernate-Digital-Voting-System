package com.securevote.backend.repository;

import com.securevote.backend.entity.Vote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByVoterIdAndElectionId(Long voterId, Long electionId);

    Optional<Vote> findByVoterIdAndElectionId(Long voterId, Long electionId);

    @Query("select v.candidate.id, count(v.id) from Vote v where v.election.id = :electionId group by v.candidate.id")
    List<Object[]> countVotesByElection(@Param("electionId") Long electionId);

        @Query("""
            select v.candidate.name, v.candidate.party, count(v.id)
            from Vote v
            where v.election.id = :electionId
            group by v.candidate.name, v.candidate.party
            order by count(v.id) desc
            """)
        List<Object[]> countVotesByCandidateName(@Param("electionId") Long electionId);

        @Query("""
            select v.candidate.region, count(v.id)
            from Vote v
            where v.election.id = :electionId
            group by v.candidate.region
            order by count(v.id) desc
            """)
        List<Object[]> countVotesByRegion(@Param("electionId") Long electionId);

        @Query("select count(v.id) from Vote v where v.election.id = :electionId")
        long countByElectionId(@Param("electionId") Long electionId);

        @Query("select v from Vote v where v.election.id = :electionId order by v.castAt desc")
        Page<Vote> findRecentByElection(@Param("electionId") Long electionId, Pageable pageable);

    List<Vote> findByElectionId(Long electionId);
}
