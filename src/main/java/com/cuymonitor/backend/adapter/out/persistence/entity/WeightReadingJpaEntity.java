package com.cuymonitor.backend.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "weight_reading")
public class WeightReadingJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cage_id", nullable = false)
    private CageJpaEntity cage;

    @Column(name = "grams", nullable = false, precision = 7, scale = 1)
    private BigDecimal grams;

    @Column(name = "stable", nullable = false)
    private boolean stable;

    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;

    protected WeightReadingJpaEntity() {
    }

    public WeightReadingJpaEntity(Long id, CageJpaEntity cage, BigDecimal grams, boolean stable,
                                  Instant measuredAt) {
        this.id = id;
        this.cage = cage;
        this.grams = grams;
        this.stable = stable;
        this.measuredAt = measuredAt;
    }

    public Long getId() {
        return id;
    }

    public CageJpaEntity getCage() {
        return cage;
    }

    public BigDecimal getGrams() {
        return grams;
    }

    public boolean isStable() {
        return stable;
    }

    public Instant getMeasuredAt() {
        return measuredAt;
    }
}
