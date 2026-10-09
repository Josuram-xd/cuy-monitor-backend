package com.cuymonitor.backend.adapter.out.persistence.entity;

import com.cuymonitor.backend.domain.model.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event")
public class EventJpaEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private EventType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cage_id", nullable = false)
    private CageJpaEntity cage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guinea_pig_id")
    private GuineaPigJpaEntity guineaPig;

    @Column(name = "source", nullable = false, length = 30)
    private String source;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, insertable = false, updatable = false)
    private Instant receivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    protected EventJpaEntity() {
    }

    public EventJpaEntity(UUID id, EventType type, CageJpaEntity cage, GuineaPigJpaEntity guineaPig, String source,
                          Instant occurredAt, JsonNode payload) {
        this.id = id;
        this.type = type;
        this.cage = cage;
        this.guineaPig = guineaPig;
        this.source = source;
        this.occurredAt = occurredAt;
        this.payload = payload;
    }

    public UUID getId() {
        return id;
    }

    public EventType getType() {
        return type;
    }

    public CageJpaEntity getCage() {
        return cage;
    }

    public GuineaPigJpaEntity getGuineaPig() {
        return guineaPig;
    }

    public String getSource() {
        return source;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public JsonNode getPayload() {
        return payload;
    }
}
