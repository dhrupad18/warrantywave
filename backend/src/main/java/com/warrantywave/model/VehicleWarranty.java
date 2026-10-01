package com.warrantywave.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "warranties")
public class VehicleWarranty extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    public Vehicle vehicle;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    public WarrantyPlan plan;

    public LocalDate startDate;
    public LocalDate endDate;

    @Enumerated(EnumType.STRING)
    public WarrantyStatus status = WarrantyStatus.ACTIVE;

    public VehicleWarranty() { }
}