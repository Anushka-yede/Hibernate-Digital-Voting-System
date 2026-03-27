package com.securevote.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_created_at", columnList = "createdAt"),
        @Index(name = "idx_audit_actor", columnList = "actor")
})
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String actor;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(length = 800)
    private String details;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
