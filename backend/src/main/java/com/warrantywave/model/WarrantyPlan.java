package com.warrantywave.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "warranty_plans")
public class WarrantyPlan extends BaseEntity {
    @Column(nullable = false, unique = true, length = 120)
    public String name;

    @Column(nullable = false, length = 1000)
    public String coveredParts;

    @Column(nullable = false)
    public int durationMonths;

    @Column(nullable = false)
    public int kmLimit;

    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal price;

    @Column(nullable = false)
    public boolean active = true;

    public WarrantyPlan() { }
}