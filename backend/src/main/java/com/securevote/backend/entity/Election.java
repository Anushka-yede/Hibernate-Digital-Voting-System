package com.securevote.backend.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "elections")
@Getter
@Setter
public class Election {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ElectionType type;

    @Column(nullable = false, length = 120)
    private String region;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ElectionStatus status = ElectionStatus.DRAFT;

    @Column(name = "start_time", nullable = false)
    private Instant startDate;

    @Column(name = "end_time", nullable = false)
    private Instant endDate;

    @Version
    private Long version;

    @OneToMany(mappedBy = "election", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Candidate> candidates = new ArrayList<>();

    @OneToMany(mappedBy = "election", fetch = FetchType.LAZY)
    private List<Vote> votes = new ArrayList<>();
}
