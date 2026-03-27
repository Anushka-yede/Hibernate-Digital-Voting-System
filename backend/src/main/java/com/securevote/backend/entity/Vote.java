package com.securevote.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "votes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_vote_user_election", columnNames = {"voter_id", "election_id"})
})
@Getter
@Setter
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "election_id", nullable = false)
    private Election election;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(nullable = false, length = 100)
    private String voteHash;

    @Column(length = 100)
    private String blockchainTxHash;

    @Column(nullable = false)
    private Instant castAt = Instant.now();

    @Version
    private Long version;
}
