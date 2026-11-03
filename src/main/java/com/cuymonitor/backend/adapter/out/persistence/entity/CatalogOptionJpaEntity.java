package com.cuymonitor.backend.adapter.out.persistence.entity;

import com.cuymonitor.backend.domain.model.CatalogKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "catalog_option")
@IdClass(CatalogOptionJpaEntity.Key.class)
public class CatalogOptionJpaEntity {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private CatalogKind kind;

    @Id
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "label", nullable = false, length = 60)
    private String label;

    // CHAR(7) in V10: a hex color always has 7 characters ("#2e9e44")
    @Column(name = "hex", length = 7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String hex;

    @Column(name = "sort_order", nullable = false)
    private short sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected CatalogOptionJpaEntity() {
    }

    public CatalogKind getKind() {
        return kind;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getHex() {
        return hex;
    }

    public short getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public record Key(CatalogKind kind, String code) implements Serializable {

        public Key {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(code, "code");
        }
    }
}
