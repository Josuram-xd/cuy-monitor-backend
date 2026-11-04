package com.cuymonitor.backend.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "baseline_profile")
public class BaselineProfileJpaEntity {
    @Id
    @Column(name = "guinea_pig_id")
    private Long guineaPigId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guinea_pig_id", nullable = false)
    private GuineaPigJpaEntity guineaPig;

    @Column(name = "avg_still_seconds", nullable = false, precision = 6, scale = 2)
    private BigDecimal avgStillSeconds;

    @Column(name = "avg_feeder_visits", nullable = false, precision = 6, scale = 2)
    private BigDecimal avgFeederVisits;

    @Column(name = "avg_group_distance", nullable = false, precision = 5, scale = 4)
    private BigDecimal avgGroupDistance;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // V13: how many normal windows it was learned from
    @Column(name = "sample_count", nullable = false)
    private int sampleCount;

    protected BaselineProfileJpaEntity() {
    }

    public BaselineProfileJpaEntity(GuineaPigJpaEntity guineaPig, BigDecimal avgStillSeconds,
                                    BigDecimal avgFeederVisits, BigDecimal avgGroupDistance, Instant updatedAt) {
        this(guineaPig, avgStillSeconds, avgFeederVisits, avgGroupDistance, updatedAt, 0);
    }

    public BaselineProfileJpaEntity(GuineaPigJpaEntity guineaPig, BigDecimal avgStillSeconds,
                                    BigDecimal avgFeederVisits, BigDecimal avgGroupDistance, Instant updatedAt,
                                    int sampleCount) {
        // the id is left to @MapsId: a null id is what tells Spring Data to persist instead of merge
        this.guineaPig = guineaPig;
        this.avgStillSeconds = avgStillSeconds;
        this.avgFeederVisits = avgFeederVisits;
        this.avgGroupDistance = avgGroupDistance;
        this.updatedAt = updatedAt;
        this.sampleCount = sampleCount;
    }

    /** A profile learned one more window: the row is updated in place. */
    public void relearn(BigDecimal avgStillSeconds, BigDecimal avgFeederVisits, BigDecimal avgGroupDistance,
                        Instant updatedAt, int sampleCount) {
        this.avgStillSeconds = avgStillSeconds;
        this.avgFeederVisits = avgFeederVisits;
        this.avgGroupDistance = avgGroupDistance;
        this.updatedAt = updatedAt;
        this.sampleCount = sampleCount;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public Long getGuineaPigId() {
        return guineaPigId;
    }

    public GuineaPigJpaEntity getGuineaPig() {
        return guineaPig;
    }

    public BigDecimal getAvgStillSeconds() {
        return avgStillSeconds;
    }

    public BigDecimal getAvgFeederVisits() {
        return avgFeederVisits;
    }

    public BigDecimal getAvgGroupDistance() {
        return avgGroupDistance;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
