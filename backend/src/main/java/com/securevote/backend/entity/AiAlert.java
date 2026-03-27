package com.securevote.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_alerts")
@Getter
@Setter
public class AiAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long electionId;

    @Column(nullable = false)
    private boolean suspicious;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
