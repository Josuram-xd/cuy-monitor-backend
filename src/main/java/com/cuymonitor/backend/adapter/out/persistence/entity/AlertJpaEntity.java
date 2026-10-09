package com.cuymonitor.backend.adapter.out.persistence.entity;

import com.cuymonitor.backend.domain.model.AlertStatus;
import com.cuymonitor.backend.domain.model.EventType;
import com.cuymonitor.backend.domain.model.HealthStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "alert")
public class AlertJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cage_id", nullable = false)
    private CageJpaEntity cage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guinea_pig_id")
    private GuineaPigJpaEntity guineaPig;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private HealthStatus level;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private EventType type;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AlertStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected AlertJpaEntity() {
    }

    public AlertJpaEntity(Long id, CageJpaEntity cage, GuineaPigJpaEntity guineaPig, HealthStatus level,
                          EventType type, String message, AlertStatus status, Instant createdAt, Instant reviewedAt) {
        this.id = id;
        this.cage = cage;
        this.guineaPig = guineaPig;
        this.level = level;
        this.type = type;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    public Long getId() {
        return id;
    }

    public CageJpaEntity getCage() {
        return cage;
    }

    public GuineaPigJpaEntity getGuineaPig() {
        return guineaPig;
    }

    public HealthStatus getLevel() {
        return level;
    }

    public EventType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
