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
@Table(name = "guinea_pig")
public class GuineaPigJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cage_id", nullable = false)
    private CageJpaEntity cage;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "mark_color", nullable = false, length = 20)
    private String markColor;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 20)
    private HealthStatus status;

    @Column(name = "status_since", nullable = false)
    private Instant statusSince;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "breed", length = 20)
    private String breed;

    @Column(name = "coat_color", length = 20)
    private String coatColor;

    @Column(name = "initial_weight_grams")
    private Integer initialWeightGrams;

    @Column(name = "notes", length = 500)
    private String notes;

    protected GuineaPigJpaEntity() {
    }

    public GuineaPigJpaEntity(Long id, CageJpaEntity cage, String name, String markColor, HealthStatus status,
                              Instant statusSince, boolean active, Instant createdAt) {
        this(id, cage, name, markColor, status, statusSince, active, createdAt, null, null, null, null);
    }

    public GuineaPigJpaEntity(Long id, CageJpaEntity cage, String name, String markColor, HealthStatus status,
                              Instant statusSince, boolean active, Instant createdAt, String breed,
                              String coatColor, Integer initialWeightGrams, String notes) {
        this.id = id;
        this.cage = cage;
        this.name = name;
        this.markColor = markColor;
        this.status = status;
        this.statusSince = statusSince;
        this.active = active;
        this.createdAt = createdAt;
        this.breed = breed;
        this.coatColor = coatColor;
        this.initialWeightGrams = initialWeightGrams;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public CageJpaEntity getCage() {
        return cage;
    }

    public String getName() {
        return name;
    }

    public String getMarkColor() {
        return markColor;
    }

    public HealthStatus getStatus() {
        return status;
    }

    public Instant getStatusSince() {
        return statusSince;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getBreed() {
        return breed;
    }

    public String getCoatColor() {
        return coatColor;
    }

    public Integer getInitialWeightGrams() {
        return initialWeightGrams;
    }

    public String getNotes() {
        return notes;
    }
}
