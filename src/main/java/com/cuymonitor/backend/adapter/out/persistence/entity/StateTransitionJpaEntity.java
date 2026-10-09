package com.cuymonitor.backend.adapter.out.persistence.entity;

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
@Table(name = "state_transition")
public class StateTransitionJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guinea_pig_id", nullable = false)
    private GuineaPigJpaEntity guineaPig;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 20)
    private HealthStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private HealthStatus toStatus;

    @Column(name = "reason", nullable = false, length = 300)
    private String reason;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected StateTransitionJpaEntity() {
    }

    public StateTransitionJpaEntity(Long id, GuineaPigJpaEntity guineaPig, HealthStatus fromStatus,
                                    HealthStatus toStatus, String reason, Instant occurredAt) {
        this.id = id;
        this.guineaPig = guineaPig;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public GuineaPigJpaEntity getGuineaPig() {
        return guineaPig;
    }

    public HealthStatus getFromStatus() {
        return fromStatus;
    }

    public HealthStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
