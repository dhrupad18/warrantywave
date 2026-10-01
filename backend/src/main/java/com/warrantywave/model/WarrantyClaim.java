package com.warrantywave.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_claims_warranty_status", columnList = "warranty_id,status"),
    @Index(name = "idx_claims_status", columnList = "status")
})
public class WarrantyClaim extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warranty_id", nullable = false)
    public VehicleWarranty warranty;

    @Column(nullable = false, length = 100)
    public String part;

    @Column(nullable = false, length = 2000)
    public String description;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal costEstimate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public ClaimStatus status;

    @Column(length = 500)
    public String validationNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    public UserAccount reviewer;

    @Column(length = 1000)
    public String reviewNote;

    public LocalDateTime reviewedAt;

    public WarrantyClaim() { }
}